# Reactor 模型

Reactor 模型在 Linux 系统中的具体实现即是 select/poll/epoll/kqueue，像 Redis 中即是采用了 Reactor 模型实现了单进程单线程高并发。Reactor 模型的理论基础可以参考 [reactor-siemens](http://www.dre.vanderbilt.edu/%7Eschmidt/PDF/reactor-siemens.pdf)

# 线程模型

## 单线程模型

所有的IO操作都在同一个NIO线程上面完成，NIO线程需要处理客户端的TCP连接，同时读取客户端Socket的请求或者应答消息以及向客户端发送请求或者应答消息。如下图：

![](https://i.postimg.cc/cLws0kS8/1fdcd36e76359339539a507278f566d7.png)

由于采用的是非阻塞的IO，所有IO操作都不会导致阻塞，从理论上来说，一个线程可以独立处理所有的IO相关操作，处理流程如下:

![](https://i.postimg.cc/zfNqBwz2/65cdba67cfcee3302b88d114e2fd5baf.png)

可以看出，单线程模型只适用并发量比较小的应用场景。当一个NIO线程同时处理上万个连接时，处理速度会变慢，会导致大量的客户端连接超时，超时之后如果进行重发，更加会加重了NIO线程的负载，最终会有大量的消息积压和处理超时，NIO线程会成为系统的瓶颈。

## 多线程模型

多线程模型与单线程模型最大的区别是有专门的一组NIO线程处理IO操作，一般使用线程池的方式实现。一个NIO线程同时处理多条连接，一个连接只能属于1个NIO线程，这样能够防止并发操作问题。

![](https://i.postimg.cc/s2JsZB1j/fbd2af5606580061718cb69254f95a71.png)

## 主从多线程模型

服务端用于接收客户端连接的不是1个单独的NIO线程了，而是采用独立的NIO线程池。Acceptor接收TCP连接请求处理完成之后，将创建新的SocketChannel注册到处理连接的IO线程池中的某个IO线程上，有它去处理IO读写以及编解码的工作。Acceptor只用于客户端登录、握手以及认证，一旦连接成功之后，将链路注册到线程池的IO线程上。

![](https://i.postimg.cc/SsNqLyzW/e774d586cd02cf2d4e7adba4b8300eac.png)

# 实现逻辑

## 核心组件

![](http://www.dengshenyu.com/assets/redis-reactor/reactor-mode3.png)

- Handles ：表示操作系统管理的资源，我们可以理解为 fd。

- Synchronous Event Demultiplexer ：同步事件分离器，阻塞等待 Handles 中的事件发生。

- Initiation Dispatcher ：初始分派器，作用为添加 Event handler(事件处理器)、删除 Event handler 以及分派事件给 Event handler。也就是说，Synchronous Event Demultiplexer 负责等待新事件发生，事件发生时通知 Initiation Dispatcher，然后 Initiation Dispatcher 调用 event handler 处理事件。

- Event Handler ：事件处理器的接口

- Concrete Event Handler ：事件处理器的实际实现，而且绑定了一个 Handle。因为在实际情况中，我们往往不止一种事件处理器，因此这里将事件处理器接口和实现分开，与 C++、Java 这些高级语言中的多态类似。

## 处理逻辑

Reactor 模型的基本的处理逻辑为：(1)我们注册 Concrete Event Handler 到 Initiation Dispatcher 中。(2)Initiation Dispatcher 调用每个 Event Handler 的 get_handle 接口获取其绑定的 Handle。(3)Initiation Dispatcher 调用 handle_events 开始事件处理循环。在这里，Initiation Dispatcher 会将步骤 2 获取的所有 Handle 都收集起来，使用 Synchronous Event Demultiplexer 来等待这些 Handle 的事件发生。(4)当某个(或某几个)Handle 的事件发生时，Synchronous Event Demultiplexer 通知 Initiation Dispatcher。(5)Initiation Dispatcher 根据发生事件的 Handle 找出所对应的 Handler。(6)Initiation Dispatcher 调用 Handler 的 handle_event 方法处理事件。

时序图如下：
![](http://www.dengshenyu.com/assets/redis-reactor/reactor-mode4.png)

抽象来说，Reactor 有 4 个核心的操作：

1. add 添加 socket 监听到 reactor，可以是 listen socket 也可以使客户端 socket，也可以是管道、eventfd、信号等
2. set 修改事件监听，可以设置监听的类型，如可读、可写。可读很好理解，对于 listen socket 就是有新客户端连接到来了需要 accept。对于客户端连接就是收到数据，需要 recv。可写事件比较难理解一些。一个 SOCKET 是有缓存区的，如果要向客户端连接发送 2M 的数据，一次性是发不出去的，操作系统默认 TCP 缓存区只有 256K。一次性只能发 256K，缓存区满了之后 send 就会返回 EAGAIN 错误。这时候就要监听可写事件，在纯异步的编程中，必须去监听可写才能保证 send 操作是完全非阻塞的。
3. del 从 reactor 中移除，不再监听事件
4. callback 就是事件发生后对应的处理逻辑，一般在 add/set 时制定。C 语言用函数指针实现，JS 可以用匿名函数，PHP 可以用匿名函数、对象方法数组、字符串函数名。

Reactor 只是一个事件发生器，实际对 socket 句柄的操作，如 connect/accept、send/recv、close 是在 callback 中完成的。具体编码可参考下面的 Swoole 伪代码：

![](http://rango.swoole.com/static/io/6.png)

Reactor 模型还可以与多进程、多线程结合起来用，既实现异步非阻塞 IO，又利用到多核。目前流行的异步服务器程序都是这样的方式：如

- Nginx：多进程 Reactor
- Nginx+Lua：多进程 Reactor+协程
- Golang：单线程 Reactor+多线程协程
- Swoole：多线程 Reactor+多进程 Worker

协程从底层技术角度看实际上还是异步 IO Reactor 模型，应用层自行实现了任务调度，借助 Reactor 切换各个当前执行的用户态线程，但用户代码中完全感知不到 Reactor 的存在。

# 链接

- https://blog.csdn.net/u013074465/article/details/46276967
