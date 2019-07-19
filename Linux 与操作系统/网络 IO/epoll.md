# epoll/kqueue

服务器的特点是经常维护着大量连接，但其中某一时刻读写的操作符数量却不多。epoll 先通过 epoll_ctl 注册一个描述符到内核中，并一直维护着而不像 poll 每次操作都将所有要监控的描述符传递给内核；在描述符读写就绪时，通过回掉函数将自己加入就绪队列中，之后 epoll_wait 返回该就绪队列。也就是说，epoll 基本不做无用的操作，时间复杂度仅与活跃的客户端数有关，而不会随着描述符数目的增加而下降。

## select 不足与 epoll 中的改进

select 与 poll 问题的关键在于无状态。对于每一次系统调用，内核不会记录下任何信息，所以每次调用都需要重复传递相同信息。总结而言，select/poll 模型存在的问题即是每次调用 select，都需要把 fd 集合从用户态拷贝到内核态，这个开销在 fd 很多时会很大并且每次都需要在内核遍历传递进来的所有的 fd，这个开销在 fd 很多时候也很大。讨论 epoll 对于 select/poll 改进的时候，epoll 和 select 和 poll 的调用接口上的不同，select 和 poll 都只提供了一个函数——select 或者 poll 函数。而 epoll 提供了三个函数，epoll_create,epoll_ctl 和 epoll_wait，epoll_create 是创建一个 epoll 句柄；epoll_ctl 是注册要监听的事件类型；epoll_wait 则是等待事件的产生。对于上面所说的 select/poll 的缺点，主要是在 epoll_ctl 中解决的，每次注册新的事件到 epoll 句柄中时(在 epoll_ctl 中指定 EPOLL_CTL_ADD)，会把所有的 fd 拷贝进内核，而不是在 epoll_wait 的时候重复拷贝。epoll 保证了每个 fd 在整个过程中只会拷贝一次。epoll 的解决方案不像 select 或 poll 一样每次都把 current 轮流加入 fd 对应的设备等待队列中，而只在 epoll_ctl 时把 current 挂一遍(这一遍必不可少)并为每个 fd 指定一个回调函数，当设备就绪，唤醒等待队列上的等待者时，就会调用这个回调函数，而这个回调函数会 把就绪的 fd 加入一个就绪链表)。epoll_wait 的工作实际上就是在这个就绪链表中查看有没有就绪的 fd(利用 schedule_timeout()实现睡一会，判断一会的效果，和 select 实现中的第 7 步是类似的)。

(1)select，poll 实现需要自己不断轮询所有 fd 集合，直到设备就绪，期间可能要睡眠和唤醒多次交替。而 epoll 其实也需要调用 epoll_wait 不断轮询就绪链表，期间也可能多次睡眠和唤醒交替，但是它是设备就绪时，调用回调函数，把就绪 fd 放入就绪链表中，并唤醒在 epoll_wait 中进入睡眠的进程。虽然都要睡眠和交替，但是 select 和 poll 在“醒着”的时候要遍历整个 fd 集合，而 epoll 在“醒着”的时候只要判断一下就绪链表是否为空就行了，这节省了大量的 CPU 时间。这就是回调机制带来的性能提升。(2)select，poll 每次调用都要把 fd 集合从用户态往内核态拷贝一次，并且要把 current 往设备等待队列中挂一次，而 epoll 只要一次拷贝，而且把 current 往等待队列上挂也只挂一次(在 epoll_wait 的开始，注意这里的等待队列并不是设备等待队列，只是一个 epoll 内部定义的等待队列)。这也能节省不少的开销。

# 函数分析

## int epoll_create(int size);

创建一个 epoll 的句柄，size 用来告诉内核这个监听的数目一共有多大。这个 参数不同于 select()中的第一个参数，给出最大监听的 fd+1 的值。需要注意的是，当创建好 epoll 句柄后，它就是会占用一个 fd 值，在 linux 下如果查看/proc/进程 id/fd/，是能够看到这个 fd 的，所以在使用完 epoll 后，必须调用 close()关闭，否则可能导致 fd 被 耗尽。

## int epoll_ctl(int epfd, int op, int fd, struct epoll_event \*event);

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
