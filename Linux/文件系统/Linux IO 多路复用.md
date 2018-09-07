# Linux IO Multiplexing | Linux IO 多路复用

select，poll，epoll 都是 IO 多路复用的机制。IO 多路复用就通过一种机制，可以监视多个描述符，一旦某个描述符就绪(一般是读就绪或者写就绪)，能够通知程序进行相应的读写操作。但 select，poll，epoll 本质上都是同步 IO，因为他们都需要在读写事件就绪后自己负责进行读写，也就是说这个读写过程是阻塞的，而异步 IO 则无需自己负责进行读写，异步 IO 的实现会负责把数据从内核拷贝到用户空间。

select 本身是轮询式、无状态的，每次调用都需要把 fd 集合从用户态拷贝到内核态，这个开销在 fd 很多时会很大。epoll 则是触发式处理连接，维护的描述符数目不受到限制，而且性能不会随着描述符数目的增加而下降。

| 方法   | 数量限制                                                                                            | 连接处理                                                                                                                   | 内存操作                                                                                                                  |
| ------ | --------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| select | 描述符个数由内核中的 FD_SETSIZE 限制，仅为 1024；重新编译内核改变 FD_SETSIZE 的值，但是无法优化性能 | 每次调用 select 都会线性扫描所有描述符的状态，在 select 结束后，用户也要线性扫描 fd_set 数组才知道哪些描述符准备就绪(O(n)) | 每次调用 select 都要在用户空间和内核空间里进行内存复制 fd 描述符等信息                                                    |
| poll   | 使用 pollfd 结构来存储 fd，突破了 select 中描述符数目的限制                                         | 类似于 select 扫描方式                                                                                                     | 需要将 pollfd 数组拷贝到内核空间，之后依次扫描 fd 的状态，整体复杂度依然是 O(n)的，在并发量大的情况下服务器性能会快速下降 |
| epoll  | 该模式下的 Socket 对应的 fd 列表由一个数组来保存，大小不限制(默认 4k)                               | 基于内核提供的反射模式，有活跃 Socket 时，内核访问该 Socket 的 callback，不需要遍历轮询                                    | epoll 在传递内核与用户空间的消息时使用了内存共享，而不是内存拷贝，这也使得 epoll 的效率比 poll 和 select 更高             |

# select/poll

![](http://images.cnitblog.com/blog/305504/201308/17201205-8ac47f1f1fcd4773bd4edd947c0bb1f4.png)

```c
// 绑定监听符
bind(lfd, (struct sockaddr *)&serv_addr, sizeof(serv_addr));

// 执行循环阻塞式监听与读取
while (1)
{
    clin_len = sizeof(clin_addr);
    cfd = accept(lfd, (struct sockaddr *)&clin_addr, &clin_len);
    while (len = read(cfd, recvbuf, BUFSIZE))
    {
        write(STDOUT_FILENO, recvbuf, len); //把客户端输入的内容输出在终端
        // 只有当客户端输入 stop 就停止当前客户端的连接
        if (strncasecmp(recvbuf, "stop", 4) == 0)
        {
            close(cfd);
            break;
        }
    }
}
```

编译运行之后，开启两个终端使用命令 `nc 10.211.55.4 8031` (假如服务器的 ip 为 10.211.55.4)。如果首先连上的客户端一直不输入`stop`加回车，那么第二个客户端输入任何内容都不会被客户端接收。

输入`abc`的是先连接上的，在其输入`stop`之前，后面连接上的客户端输入`123`并不会被服务端收到。也就是说一直阻塞在第一个客户端那里。当第一个客户端输入`stop`之后，服务端才收到第二个客户端的发送过来的数据。

## 函数分析

```
select(int nfds, fd_set *r, fd_set *w, fd_set *e, struct timeval *timeout)
```

- `maxfdp1`表示该进程中描述符的总数。

- `fd_set`则是配合`select`模型的重点数据结构，用来存放描述符的集合。

- `timeout`表示`select`返回需要等待的时间。

对于 select()，我们需要传 3 个集合，r，w 和 e。其中，r 表示我们对哪些 fd 的可读事件感兴趣，w 表示我们对哪些 fd 的可写事件感兴趣。每个集合其实是一个 bitmap，通过 0/1 表示我们感兴趣的 fd。例如，我们对于 fd 为 6 的可读事件感兴趣，那么 r 集合的第 6 个 bit 需要被 设置为 1。这个系统调用会阻塞，直到我们感兴趣的事件(至少一个)发生。调用返回时，内核同样使用这 3 个集合来存放 fd 实际发生的事件信息。也就是说，调 用前这 3 个集合表示我们感兴趣的事件，调用后这 3 个集合表示实际发生的事件。

select 为最早期的 UNIX 系统调用，它存在 4 个问题：1)这 3 个 bitmap 有大小限制(FD_SETSIZE，通常为 1024)；2)由于 这 3 个集合在返回时会被内核修改，因此我们每次调用时都需要重新设置；3)我们在调用完成后需要扫描这 3 个集合才能知道哪些 fd 的读/写事件发生了，一般情况下全量集合比较大而实际发生读/写事件的 fd 比较少，效率比较低下；4)内核在每次调用都需要扫描这 3 个 fd 集合，然后查看哪些 fd 的事件实际发生， 在读/写比较稀疏的情况下同样存在效率问题。

由于存在这些问题，于是人们对 select 进行了改进，从而有了 poll。

```
poll(struct pollfd *fds, int nfds, int timeout)
struct pollfd {
    int fd;
    short events;
    short revents;
    }
```

poll 调用需要传递的是一个 pollfd 结构的数组，调用返回时结果信息也存放在这个数组里面。 pollfd 的结构中存放着 fd、我们对该 fd 感兴趣的事件(events)以及该 fd 实际发生的事件(revents)。poll 传递的不是固定大小的 bitmap，因此 select 的问题 1 解决了；poll 将感兴趣事件和实际发生事件分开了，因此 select 的问题 2 也解决了。但 select 的问题 3 和问题 4 仍然没有解决。

## 处理逻辑

总的来说，Select 模型的内核的处理逻辑为：

(1)使用 copy_from_user 从用户空间拷贝 fd_set 到内核空间(2)注册回调函数**pollwait
(3)遍历所有 fd，调用其对应的 poll 方法(对于 socket，这个 poll 方法是 sock_poll，sock_poll 根据情况会调用到 tcp_poll,udp_poll 或者 datagram_poll)(4)以 tcp_poll 为例，其核心实现就是**pollwait，也就是上面注册的回调函数。(5)\_\_pollwait 的主要工作就是把 current(当前进程)挂到设备的等待队列中，不同的设备有不同的等待队列，对于 tcp_poll 来说，其等待队列是 sk->sk_sleep(注意把进程挂到等待队列中并不代表进程已经睡眠了)。在设备收到一条消息(网络设备)或填写完文件数 据(磁盘设备)后，会唤醒设备等待队列上睡眠的进程，这时 current 便被唤醒了。(6)poll 方法返回时会返回一个描述读写操作是否就绪的 mask 掩码，根据这个 mask 掩码给 fd_set 赋值。(7)如果遍历完所有的 fd，还没有返回一个可读写的 mask 掩码，则会调用 schedule_timeout 是调用 select 的进程(也就是 current)进入睡眠。当设备驱动发生自身资源可读写后，会唤醒其等待队列上睡眠的进程。如果超过一定的超时时间(schedule_timeout 指定)，还是没人唤醒，则调用 select 的进程会重新被唤醒获得 CPU，进而重新遍历 fd，判断有没有就绪的 fd。(8)把 fd_set 从内核空间拷贝到用户空间。多客户端请求服务端，服务端与各客户端保持长连接并且能接收到各客户端数据大体思路如下：

(1)初始化`readset`，并且将服务端监听的描述符添加到`readset`中去。

(2)然后`select`阻塞等待`readset`集合中是否有描述符可读。

(3)如果是服务端描述符可读，那么表示有新客户端连接上。通过`accept`接收客户端的数据，并且将客户端描述符添加到一个数组`client`中，以便二次遍历的时候使用。

(4)执行第二次循环，此时通过`for`循环把`client`中的有效的描述符都添加到`readset`中去。

(5)`select`再次阻塞等待`readset`集合中是否有描述符可读。

(6)如果此时已经连接上的某个客户端描述符有数据可读，则进行数据读取。

>

```c
while (1)
{
    // 每次循环开始时，都初始化 read_set
    read_set = read_set_init;

    // 因为上一步 read_set 已经重置，所以需要已连接上的客户端 fd (由上次循环后产生)重新添加进 read_set
    for (i = 0; i < FD_SET_SIZE; ++i)
    {
        if (client[i] > 0)
        {
            FD_SET(client[i], &read_set);
        }
    }

    ...

    // 这里会阻塞，直到 read_set 中某一个 fd 有数据可读才返回，注意 read_set 中除了客户端 fd 还有服务端监听的 fd
    retval = select(maxfd + 1, &read_set, NULL, NULL, NULL);

    ...

    // 用 FD_ISSET 来判断 lfd (服务端监听的fd)是否可读。只有当新的客户端连接时，lfd 才可读
    if (FD_ISSET(lfd, &read_set))
    {
        ...
    }

    for (i = 0; i < maxi; ++i)
    {
        if (client[i] < 0)
        {
            continue;
        }

        // 如果客户端 fd 中有数据可读，则进行读取
        if (FD_ISSET(client[i], &read_set))
        {
            // 注意：这里没有使用 while 循环读取，如果使用 while 循环读取，则有阻塞在一个客户端了。
            // 可能你会想到如果一次读取不完怎么办？
            // 读取不完时，在循环到 select 时 由于未读完的 fd 还有数据可读，那么立即返回，然后到这里继续读取，原来的 while 循环读取直接提到最外层的 while(1) + select 来判断是否有数据继续可读
            ...
        }
    }
}
```

# epoll/kqueue

服务器的特点是经常维护着大量连接，但其中某一时刻读写的操作符数量却不多。epoll 先通过 epoll_ctl 注册一个描述符到内核中，并一直维护着而不像 poll 每次操作都将所有要监控的描述符传递给内核；在描述符读写就绪时，通过回掉函数将自己加入就绪队列中，之后 epoll_wait 返回该就绪队列。也就是说，epoll 基本不做无用的操作，时间复杂度仅与活跃的客户端数有关，而不会随着描述符数目的增加而下降。

## select 不足与 epoll 中的改进

select 与 poll 问题的关键在于无状态。对于每一次系统调用，内核不会记录下任何信息，所以每次调用都需要重复传递相同信息。总结而言，select/poll 模型存在的问题即是每次调用 select，都需要把 fd 集合从用户态拷贝到内核态，这个开销在 fd 很多时会很大并且每次都需要在内核遍历传递进来的所有的 fd，这个开销在 fd 很多时候也很大。讨论 epoll 对于 select/poll 改进的时候，epoll 和 select 和 poll 的调用接口上的不同，select 和 poll 都只提供了一个函数——select 或者 poll 函数。而 epoll 提供了三个函数，epoll_create,epoll_ctl 和 epoll_wait，epoll_create 是创建一个 epoll 句柄；epoll_ctl 是注册要监听的事件类型；epoll_wait 则是等待事件的产生。对于上面所说的 select/poll 的缺点，主要是在 epoll_ctl 中解决的，每次注册新的事件到 epoll 句柄中时(在 epoll_ctl 中指定 EPOLL_CTL_ADD)，会把所有的 fd 拷贝进内核，而不是在 epoll_wait 的时候重复拷贝。epoll 保证了每个 fd 在整个过程中只会拷贝一次。epoll 的解决方案不像 select 或 poll 一样每次都把 current 轮流加入 fd 对应的设备等待队列中，而只在 epoll_ctl 时把 current 挂一遍(这一遍必不可少)并为每个 fd 指定一个回调函数，当设备就绪，唤醒等待队列上的等待者时，就会调用这个回调函数，而这个回调函数会 把就绪的 fd 加入一个就绪链表)。epoll_wait 的工作实际上就是在这个就绪链表中查看有没有就绪的 fd(利用 schedule_timeout()实现睡一会，判断一会的效果，和 select 实现中的第 7 步是类似的)。

(1)select，poll 实现需要自己不断轮询所有 fd 集合，直到设备就绪，期间可能要睡眠和唤醒多次交替。而 epoll 其实也需要调用 epoll_wait 不断轮询就绪链表，期间也可能多次睡眠和唤醒交替，但是它是设备就绪时，调用回调函数，把就绪 fd 放入就绪链表中，并唤醒在 epoll_wait 中进入睡眠的进程。虽然都要睡眠和交替，但是 select 和 poll 在“醒着”的时候要遍历整个 fd 集合，而 epoll 在“醒着”的时候只要判断一下就绪链表是否为空就行了，这节省了大量的 CPU 时间。这就是回调机制带来的性能提升。(2)select，poll 每次调用都要把 fd 集合从用户态往内核态拷贝一次，并且要把 current 往设备等待队列中挂一次，而 epoll 只要一次拷贝，而且把 current 往等待队列上挂也只挂一次(在 epoll_wait 的开始，注意这里的等待队列并不是设备等待队列，只是一个 epoll 内部定义的等待队列)。这也能节省不少的开销。

## 函数分析

### int epoll_create(int size);

创建一个 epoll 的句柄，size 用来告诉内核这个监听的数目一共有多大。这个 参数不同于 select()中的第一个参数，给出最大监听的 fd+1 的值。需要注意的是，当创建好 epoll 句柄后，它就是会占用一个 fd 值，在 linux 下如果查看/proc/进程 id/fd/，是能够看到这个 fd 的，所以在使用完 epoll 后，必须调用 close()关闭，否则可能导致 fd 被 耗尽。

### int epoll_ctl(int epfd, int op, int fd, struct epoll_event \*event);

epoll 的事件注册函数，它不同与 select()是在监听事件时告诉内核要监听什么类型的事件，而是在这里先注册要监听的事件类型。第一个参数是 epoll_create()的返回值，第二个参数表示动作，用三个宏来表示：

- EPOLL_CTL_ADD：注册新的 fd 到 epfd 中；
- EPOLL_CTL_MOD：修改已经注册的 fd 的监听事件；
- EPOLL_CTL_DEL：从 epfd 中删除一个 fd；

第三个参数是需要监听的 fd，第四个参数是告诉内核需要监听什么事，struct epoll_event 结构如下：

```
typedef union epoll_data {
    void *ptr;
    int fd;
    __uint32_t u32;
    __uint64_t u64;
} epoll_data_t;

struct epoll_event {
    __uint32_t events; /* Epoll events */
    epoll_data_t data; /* User data variable */
};
```

events 可以是以下几个宏的集合：

- EPOLLIN ：表示对应的文件描述符可以读(包括对端 SOCKET 正常关闭)；
- EPOLLOUT：表示对应的文件描述符可以写；
- EPOLLPRI：表示对应的文件描述符有紧急的数据可读(这里应该表示有带外数据到来)；
- EPOLLERR：表示对应的文件描述符发生错误；
- EPOLLHUP：表示对应的文件描述符被挂断；
- EPOLLET: 将 EPOLL 设为边缘触发(Edge Triggered)模式，这是相对于水平触发(Level Triggered)来说的。
- EPOLLONESHOT：只监听一次事件，当监听完这次事件之后，如果还需要继续监听这个 socket 的话，需要再次把这个 socket 加入到 EPOLL 队列里

### int epoll_wait(int epfd, struct epoll_event \* events, int maxevents, int timeout);

等 待事件的产生，类似于 select()调用。参数 events 用来从内核得到事件的集合，maxevents 告之内核这个 events 有多大，这个 maxevents 的值不能大于创建 epoll_create()时的 size，参数 timeout 是超时时间(毫秒，0 会立即返回，-1 将不确定，也有 说法说是永久阻塞)。该函数返回需要处理的事件数目，如返回 0 表示已超时。

## 处理逻辑

使用 epoll 来实现服务端同时接受多客户端长连接数据时，的大体步骤如下：(1)使用 epoll_create 创建一个 epoll 的句柄，下例中我们命名为 epollfd。(2)使用 epoll_ctl 把服务端监听的描述符添加到 epollfd 指定的 epoll 内核事件表中，监听服务器端监听的描述符是否可读。(3)使用 epoll_wait 阻塞等待注册的服务端监听的描述符可读事件的发生。(4)当有新的客户端连接上服务端时，服务端监听的描述符可读，则 epoll_wait 返回，然后通过 accept 获取客户端描述符。(5)使用 epoll_ctl 把客户端描述符添加到 epollfd 指定的 epoll 内核事件表中，监听服务器端监听的描述符是否可读。(6)当客户端描述符有数据可读时，则触发 epoll_wait 返回，然后执行读取。

几乎所有的 epoll 模型编码都是基于以下模板：

```c
for( ; ; )
{
    // 阻塞式等待事件
    nfds = epoll_wait(epfd,events,20,500);
    for(i=0;i<nfds;++i)
    {
        if(events[i].data.fd==listenfd) //有新的连接
        {
            connfd = accept(listenfd,(sockaddr *)&clientaddr, &clilen); //accept这个连接
            ev.data.fd=connfd;
            ev.events=EPOLLIN|EPOLLET;
            epoll_ctl(epfd,EPOLL_CTL_ADD,connfd,&ev); //将新的fd添加到epoll的监听队列中
        }
        else if(events[i].events&EPOLLIN ) //接收到数据，读socket
        {
            n = read(sockfd, line, MAXLINE)) < 0    //读
            ev.data.ptr = md;     //md为自定义类型，添加数据
            ev.events=EPOLLOUT|EPOLLET;
            epoll_ctl(epfd,EPOLL_CTL_MOD,sockfd,&ev);//修改标识符，等待下一个循环时发送数据，异步处理的精髓
        }
        else if(events[i].events&EPOLLOUT) //有数据待发送，写socket
        {
            struct myepoll_data* md = (myepoll_data*)events[i].data.ptr;    //取数据
            sockfd = md->fd;
            send( sockfd, md->ptr, strlen((char*)md->ptr), 0 );        //发送数据
            ev.data.fd=sockfd;
            ev.events=EPOLLIN|EPOLLET;
            epoll_ctl(epfd,EPOLL_CTL_MOD,sockfd,&ev); //修改标识符，等待下一个循环时接收数据
        }
        else
        {
            //其他的处理
        }
    }
}
```
