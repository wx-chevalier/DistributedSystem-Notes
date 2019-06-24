# KeyDB

众所周知 Redis 是一个单线程的 KV 内存存储系统，KeyDB 项目则是从 redis fork 出来的分支，在 100% 兼容 redis API 的情况下将 redis 改造成多线程模型。

# 线程模型

KeyDB 将 redis 原来的主线程拆分成了主线程和 worker 线程。每个 worker 线程都是 io 线程，负责监听端口，accept 请求，读取数据和解析协议。如图所示：

![](https://i.postimg.cc/15X94frg/image.png)

KeyDB 的多线程模型核心要点如下：

- KeyDB 使用了 SO_REUSEPORT 特性，多个线程可以绑定监听同个端口。
- 每个 worker 线程做了 cpu 绑核，读取数据也使用了 SO_INCOMING_CPU 特性，指定 cpu 接收数据。
- 解析协议之后每个线程都会去操作内存中的数据，由一把全局锁来控制多线程访问内存数据。

主线程其实也是一个 worker 线程，包括了 worker 线程的工作内容，同时也包括只有主线程才可以完成的工作内容。在 worker 线程数组中下标为 0 的就是主线程。主线程的主要工作在实现 serverCron，包括：

- 处理统计
- 客户端链接管理
- db 数据的 resize 和 reshard
- 处理 AOF
- Replication 主备同步
- Cluster 模式下的任务

# 链接管理

在 redis 中所有链接管理都是在一个线程中完成的。在 KeyDB 的设计中，每个 worker 线程负责一组链接，所有的链接插入到本线程的链接列表中维护。链接的产生、工作、销毁必须在同个线程中。每个链接新增一个字段 `int iel; /* the event loop index we're registered with */`，用来表示链接属于哪个线程接管。

KeyDB 维护了三个关键的数据结构做链接管理：

- clients_pending_write：线程专属的链表，维护同步给客户链接发送数据的队列
- clients_pending_asyncwrite：线程专属的链表，维护异步给客户链接发送数据的队列
- clients_to_close：全局链表，维护需要异步关闭的客户链接

分成同步和异步两个队列，是因为 redis 有些联动 api，比如 pub/sub，pub 之后需要给 sub 的客户端发送消息，pub 执行的线程和 sub 的客户端所在线程不是同一个线程，为了处理这种情况，KeyDB 将需要给非本线程的客户端发送数据维护在异步队列中。

同步发送的逻辑比较简单，都是在本线程中完成，以下图来说明如何同步给客户端发送数据：

![](https://i.postimg.cc/KzQ282Kr/image.png)

一个链接的创建、接收数据、发送数据、释放链接都必须在同个线程执行。异步发送涉及到两个线程之间的交互。KeyDB 通过管道在两个线程中传递消息：

```sh
int fdCmdWrite; //写管道
int fdCmdRead; //读管道
```

本地线程需要异步发送数据时，先检查 client 是否属于本地线程，非本地线程获取到 client 专属的线程 ID，之后给专属的线程管到发送 `AE_ASYNC_OP::CreateFileEvent` 的操作，要求添加写 socket 事件。专属线程在处理管道消息时将对应的请求添加到写事件中，如图所示：

![](https://i.postimg.cc/HnxCJhXG/image.png)

redis 有些关闭客户端的请求并非完全是在链接所在的线程执行关闭，所以在这里维护了一个全局的异步关闭链表。

![](https://i.postimg.cc/wMGYDQQ1/image.png)

# 锁机制

KeyDB 实现了一套类似 spinlock 的锁机制，称之为 fastlock。fastlock 的主要数据结构有：

```c
struct ticket
{
    uint16_t m_active;  //解锁+1
    uint16_t m_avail;  //加锁+1
};
struct fastlock
{
    volatile struct ticket m_ticket;

    volatile int m_pidOwner; //当前解锁的线程id
    volatile int m_depth; //当前线程重复加锁的次数
};
```

使用原子操作 `__atomic_load_2，__atomic_fetch_add，__atomic_compare_exchange` 来通过比较 `m_active=m_avail` 判断是否可以获取锁。fastlock 提供了两种获取锁的方式：

- try_lock：一次获取失败，直接返回
- lock：忙等，每 `1024 * 1024` 次忙等后使用 sched_yield 主动交出 cpu，挪到 cpu 的任务末尾等待执行。

在 KeyDB 中将 try_lock 和事件结合起来，来避免忙等的情况发生。每个客户端有一个专属的 lock，在读取客户端数据之前会先尝试加锁，如果失败，则退出，因为数据还未读取，所以在下个 epoll_wait 处理事件循环中可以再次处理。

![](https://i.postimg.cc/hG1F2B0W/image.png)

# Active-Replica

KeyDB 实现了多活的机制，每个 replica 可设置成可写非只读，replica 之间互相同步数据。主要特性有：

- 每个 replica 有个 uuid 标志，用来去除环形复制
- 新增加 rreplay API，将增量命令打包成 rreplay 命令，带上本地的 uuid
- key，value 加上时间戳版本号，作为冲突校验，如果本地有相同的 key 且时间戳版本号大于同步过来的数据，新写入失败。采用当前时间戳向左移 20 位，再加上后 44 位自增的方式来获取 key 的时间戳版本号。
