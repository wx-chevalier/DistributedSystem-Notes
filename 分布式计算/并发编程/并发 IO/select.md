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
