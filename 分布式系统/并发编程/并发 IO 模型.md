![default](https://i.postimg.cc/V6m3yh19/image.png)

# 并发 IO 模型

在传统的网络服务器的构建中，IO 模式会按照 Blocking/Non-Blocking、Synchronous/Asynchronous 这两个标准进行分类，其中 Blocking 与 Synchronous 基本上一个意思，而 NIO 与 Async 的区别在于 NIO 强调的是 Polling(轮询)，而 Async 强调的是 Notification(通知)。譬如在一个典型的单进程单线程 Socket 接口中，阻塞型的接口必须在上一个 Socket 连接关闭之后才能接入下一个 Socket 连接。而对于 NIO 的 Socket 而言，Server Application 会从内核获取到一个特殊的"Would Block"错误信息，但是并不会阻塞到等待发起请求的 Socket Client 停止。一般来说，在 Linux 系统中可以通过调用独立的`select`或者`poll`方法来遍历所有读取好的数据，并且进行写操作。而对于异步 Socket 而言(譬如 Windows 中的 Sockets 或者.Net 中实现的 Sockets 模型)，Server Application 会告诉 IO Framework 去读取某个 Socket 数据，在数据读取完毕之后 IO Framework 会自动地调用你的回调(也就是通知应用程序本身数据已经准备好了)。以 IO 多路复用中的 Reactor 与 Proactor 模型为例，非阻塞的模型是需要应用程序本身处理 IO 的，而异步模型则是由 Kernel 或者 Framework 将数据准备好读入缓冲区中，应用程序直接从缓冲区读取数据。总结一下：

- 同步阻塞：在此种方式下，用户进程在发起一个 IO 操作以后，必须等待 IO 操作的完成，只有当真正完成了 IO 操作以后，用户进程才能运行。

- 同步非阻塞：在此种方式下，用户进程发起一个 IO 操作以后边可返回做其它事情，但是用户进程需要时不时的询问 IO 操作是否就绪，这就要求用户进程不停的去询问，从而引入不必要的 CPU 资源浪费。

- 异步非阻塞：在此种模式下，用户进程只需要发起一个 IO 操作然后立即返回，等 IO 操作真正的完成以后，应用程序会得到 IO 操作完成的通知，此时用户进程只需要对数据进行处理就好了，不需要进行实际的 IO 读写操作，因为真正的 IO 读取或者写入操作已经由内核完成了。

而在并发 IO 的问题中，较常见的就是所谓的 C10K 问题，即有 10000 个客户端需要连上一个服务器并保持 TCP 连接，客户端会不定时的发送请求给服务器，服务器收到请求后需及时处理并返回结果。

# BIO

BIO 即同步阻塞式 IO，是面向流的，阻塞式的，串行的一个过程。对每一个客户端的 socket 连接，都需要一个线程来处理，而且在此期间这个线程一直被占用，直到 socket 关闭。
![](http://7xkt0f.com1.z0.glb.clouddn.com/ed484965-b93f-4f6a-aa44-419f042d5872.png)
采用 BIO 通信模型的服务端，通常由一个独立的 Acceptor 线程负责监听客户端的连接，接收到客户端连接之后为客户端连接创建一个新的线程处理请求消息，处理完成之后，返回应答消息给客户端，线程销毁，这就是典型的一请求一应答模型。该架构最大的问题就是不具备弹性伸缩能力，当并发访问量增加后，服务端的线程个数和并发访问数成线性正比，由于线程是 JAVA 虚拟机非常宝贵的系统资源，当线程数膨胀之后，系统的性能急剧下降，随着并发量的继续增加，可能会发生句柄溢出、线程堆栈溢出等问题，并导致服务器最终宕机。还有一些并不是由于并发数增加而导致的系统负载增加：连接服务器的一些客户端，由于网络或者自身性能处理的问题，接收端从 socket 读取数据的速度跟不上发送端写入数据的速度。 而在 TCP/IP 网络编程过程中，已经发送出去的数据依然需要暂存在 send buffer，只有收到对方的 ack，kernel 才从 buffer 中清除这一部分数据，为后续发送数据腾出空间。接收端将收到的数据暂存在 receive buffer 中，自动进行确认。但如果 socket 所在的进程不及时将数据从 receive buffer 中取出，最终导致 receive buffer 填满，由于 TCP 的滑动窗口和拥塞控制，接收端会阻止发送端向其发送数据。作为发送端，服务器由于迟迟不能释放被占用的线程，导致内存占用率不断升高，堆回收的效率越来越低，导致 Full GC，最终导致服务宕机。

## 多进程/多线程模式

本文初提及，BIO 的一个缺陷在于某个 Socket 在其连接到断上期间会独占线程，那么解决这个问题的一个朴素想法就是利用多进程多线程的办法，即是创建一个新的线程来处理新的连接，这样就保证了并发 IO 的实现。本节即是对这种思路进行分析。最早的服务器端程序都是通过多进程、多线程来解决并发 IO 的问题。进程模型出现的最早，从 Unix 系统诞生就开始有了进程的概念。最早的服务器端程序一般都是 Accept 一个客户端连接就创建一个进程，然后子进程进入循环同步阻塞地与客户端连接进行交互，收发处理数据。

![](http://rango.swoole.com/static/io/4.png)

![](http://rango.swoole.com/static/io/4.png)

多线程模式出现要晚一些，线程与进程相比更轻量，而且线程之间是共享内存堆栈的，所以不同的线程之间交互非常容易实现。比如聊天室这样的程序，客户端连接之间可以交互，比聊天室中的玩家可以任意的其他人发消息。用多线程模式实现非常简单，线程中可以直接读写某一个客户端连接。而多进程模式就要用到管道、消息队列、共享内存实现数据交互，统称进程间通信(IPC)复杂的技术才能实现。
![](http://rango.swoole.com/static/io/1.png)
多进程/线程模型的流程如下：

1. 创建一个 socket，绑定服务器端口(bind)，监听端口(listen)，在 PHP 中用 stream_socket_server 一个函数就能完成上面 3 个步骤，当然也可以使用 php sockets 扩展分别实现。
2. 进入 while 循环，阻塞在 accept 操作上，等待客户端连接进入。此时程序会进入随眠状态，直到有新的客户端发起 connect 到服务器，操作系统会唤醒此进程。accept 函数返回客户端连接的 socket
3. 主进程在多进程模型下通过 fork(php: pcntl_fork)创建子进程，多线程模型下使用 pthread_create(php: new Thread)创建子线程。下文如无特殊声明将使用进程同时表示进程/线程。
4. 子进程创建成功后进入 while 循环，阻塞在 recv(php: fread)调用上，等待客户端向服务器发送数据。收到数据后服务器程序进行处理然后使用 send(php: fwrite)向客户端发送响应。长连接的服务会持续与客户端交互，而短连接服务一般收到响应就会 close。
5. 当客户端连接关闭时，子进程退出并销毁所有资源。主进程会回收掉此子进程。

## Leader-Follow 模型

![](http://www.dengshenyu.com/assets/redis-reactor/reactor-mode2.png)
上文描述的多进程/多线程模型最大的问题是，进程/线程创建和销毁的开销很大。所以上面的模式没办法应用于非常繁忙的服务器程序。对应的改进版解决了此问题，这就是经典的**Leader-Follower**模型。

![](http://rango.swoole.com/static/io/2.png)

它的特点是程序启动后就会创建 N 个进程。每个子进程进入 Accept，等待新的连接进入。当客户端连接到服务器时，其中一个子进程会被唤醒，开始处理客户端请求，并且不再接受新的 TCP 连接。当此连接关闭时，子进程会释放，重新进入 Accept，参与处理新的连接。这个模型的优势是完全可以复用进程，没有额外消耗，性能非常好。很多常见的服务器程序都是基于此模型的，比如 Apache、PHP-FPM。

多进程模型也有一些缺点。

1. 这种模型严重依赖进程的数量解决并发问题，一个客户端连接就需要占用一个进程，工作进程的数量有多少，并发处理能力就有多少。操作系统可以创建的进程数量是有限的。
2. 启动大量进程会带来额外的进程调度消耗。数百个进程时可能进程上下文切换调度消耗占 CPU 不到 1%可以忽略不接，如果启动数千甚至数万个进程，消耗就会直线上升。调度消耗可能占到 CPU 的百分之几十甚至 100%。

另外有一些场景多进程模型无法解决，比如即时聊天程序(IM)，一台服务器要同时维持上万甚至几十万上百万的连接(经典的 C10K 问题)，多进程模型就力不从心了。还有一种场景也是多进程模型的软肋。通常 Web 服务器启动 100 个进程，如果一个请求消耗 100ms，100 个进程可以提供 1000qps，这样的处理能力还是不错的。但是如果请求内要调用外网 Http 接口，像 QQ、微博登录，耗时会很长，一个请求需要 10s。那一个进程 1 秒只能处理 0.1 个请求，100 个进程只能达到 10qps，这样的处理能力就太差了。

# Unix IO 模型

Unix 中内置了 5 种 IO 模型，阻塞式 IO, 非阻塞式 IO，IO 复用模型，信号驱动式 IO 和异步 IO：

## Blocking I/O: 同步/阻塞式 IO

![](https://notes.shichao.io/unp/figure_6.1.png)

## Nonblocking I/O: 非阻塞式 IO

![](https://notes.shichao.io/unp/figure_6.2.png)

## I/O Multiplexing: IO 复用(select,poll)

I/O multiplexing means what it says - allowing the programmer to examine and block on multiple I/O streams (or other "synchronizing" events), being notified whenever any one of the streams is active so that it can process data on that stream.

In the Unix world, it's called select() or poll() (when using the CeeLanguage API for Unix). In the MicrosoftWindowsApi world, it's called WaitForMultipleObjects().

IO 多路复用通过把多个 IO 的阻塞复用到同一个 select 的阻塞上，从而使得系统在单线程的情况可以同时处理多个客户端请求。 目前支持 IO 多路复用的**系统调用**有 select，pselect，poll，epoll，在 linux 网络编程过程中，很长一段时间都是用 select 做轮询和网络事件通知，然而 select 的一些固有缺陷导致了它的应用受到了很大的限制，最终 linux 不得不载新的内核版本中寻找 select 的替代方案，最终选择了 epoll。

![](https://notes.shichao.io/unp/figure_6.3.png)

IO 多路复用技术通俗阐述，即是由一个线程轮询每个连接，如果某个连接有请求则处理请求，没有请求则处理下一个连接。首先来看下可读事件与可写事件：当如下**任一**情况发生时，会产生套接字的**可读**事件：

- 该套接字的接收缓冲区中的数据字节数大于等于套接字接收缓冲区低水位标记的大小；
- 该套接字的读半部关闭(也就是收到了 FIN)，对这样的套接字的读操作将返回 0(也就是返回 EOF)；
- 该套接字是一个监听套接字且已完成的连接数不为 0；
- 该套接字有错误待处理，对这样的套接字的读操作将返回-1。

当如下任一情况发生时，会产生套接字的可写事件：

- 该套接字的发送缓冲区中的可用空间字节数大于等于套接字发送缓冲区低水位标记的大小；
- 该套接字的写半部关闭，继续写会产生 SIGPIPE 信号；
- 非阻塞模式下，connect 返回之后，该套接字连接成功或失败；
- 该套接字有错误待处理，对这样的套接字的写操作将返回-1。

## Signal-Driven I/O Model: 信号驱动式 IO

![](https://notes.shichao.io/unp/figure_6.4.png)

## Asynchronous I/O Model: 异步 IO

Asynchronous IO refers to an interface where you supply a callback to an IO operation, which is invoked when the operation completes. This invocation often happens to an entirely different thread to the one that originally made the request, but this is not necessarily the case. Asynchronous IO is a manifestation of the ["proactor" pattern](https://en.wikipedia.org/wiki/Proactor_pattern).

![](https://notes.shichao.io/unp/figure_6.5.png)

并发 IO 问 题一直是后端编程中的技术挑战，从最早的同步阻塞 Fork 进程，到多进程/多线程，到现在的异步 IO、协程。

# Reactor 模型

Reactor 模型在 Linux 系统中的具体实现即是 select/poll/epoll/kqueue，像 Redis 中即是采用了 Reactor 模型实现了单进程单线程高并发。Reactor 模型的理论基础可以参考[reactor-siemens](http://www.dre.vanderbilt.edu/%7Eschmidt/PDF/reactor-siemens.pdf)

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

Reactor 只是一个事件发生器，实际对 socket 句柄的操作，如 connect/accept、send/recv、close 是在 callback 中完成的。具体编码可参考下面的伪代码：

![](http://rango.swoole.com/static/io/6.png)

Reactor 模型还可以与多进程、多线程结合起来用，既实现异步非阻塞 IO，又利用到多核。目前流行的异步服务器程序都是这样的方式：如

- Nginx：多进程 Reactor
- Nginx+Lua：多进程 Reactor+协程
- Golang：单线程 Reactor+多线程协程
- Swoole：多线程 Reactor+多进程 Worker

协程从底层技术角度看实际上还是异步 IO Reactor 模型，应用层自行实现了任务调度，借助 Reactor 切换各个当前执行的用户态线程，但用户代码中完全感知不到 Reactor 的存在。

# Proactor 模型

Reactor 和 Proactor 模式的主要区别就是真正的读取和写入操作是有谁来完成的，Reactor 中需要应用程序自己读取或者写入数据，而 Proactor 模式中，应用程序不需要进行实际的读写过程，它只需要从缓存区读取或者写入即可，操作系统会读取缓存区或者写入缓存区到真正的 IO 设备。Proactor 模型的基本处理逻辑如下：

1. 应用程序初始化一个异步读取操作，然后注册相应的事件处理器，此时事件处理器不关注读取就绪事件，而是关注读取完成事件，这是区别于 Reactor 的关键。
2. 事件分离器等待读取操作完成事件。
3. 在事件分离器等待读取操作完成的时候，操作系统调用内核线程完成读取操作(异步 IO 都是操作系统负责将数据读写到应用传递进来的缓冲区供应用程序操作，操作系统扮演了重要角色)，并将读取的内容放入用户传递过来的缓存区中。这也是区别于 Reactor 的一点，Proactor 中，应用程序需要传递缓存区。
4. 事件分离器捕获到读取完成事件后，激活应用程序注册的事件处理器，事件处理器直接从缓存区读取数据，而不需要进行实际的读取操作。
