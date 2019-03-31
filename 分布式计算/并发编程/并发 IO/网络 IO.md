# 网络 IO

select，poll，epoll 都是 IO 多路复用的机制。IO 多路复用就通过一种机制，可以监视多个描述符，一旦某个描述符就绪(一般是读就绪或者写就绪)，能够通知程序进行相应的读写操作。但 select，poll，epoll 本质上都是同步 IO，因为他们都需要在读写事件就绪后自己负责进行读写，也就是说这个读写过程是阻塞的，而异步 IO 则无需自己负责进行读写，异步 IO 的实现会负责把数据从内核拷贝到用户空间。

select 本身是轮询式、无状态的，每次调用都需要把 fd 集合从用户态拷贝到内核态，这个开销在 fd 很多时会很大。epoll 则是触发式处理连接，维护的描述符数目不受到限制，而且性能不会随着描述符数目的增加而下降。

| 方法   | 数量限制                                                                                            | 连接处理                                                                                                                   | 内存操作                                                                                                                  |
| ------ | --------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| select | 描述符个数由内核中的 FD_SETSIZE 限制，仅为 1024；重新编译内核改变 FD_SETSIZE 的值，但是无法优化性能 | 每次调用 select 都会线性扫描所有描述符的状态，在 select 结束后，用户也要线性扫描 fd_set 数组才知道哪些描述符准备就绪(O(n)) | 每次调用 select 都要在用户空间和内核空间里进行内存复制 fd 描述符等信息                                                    |
| poll   | 使用 pollfd 结构来存储 fd，突破了 select 中描述符数目的限制                                         | 类似于 select 扫描方式                                                                                                     | 需要将 pollfd 数组拷贝到内核空间，之后依次扫描 fd 的状态，整体复杂度依然是 O(n)的，在并发量大的情况下服务器性能会快速下降 |
| epoll  | 该模式下的 Socket 对应的 fd 列表由一个数组来保存，大小不限制(默认 4k)                               | 基于内核提供的反射模式，有活跃 Socket 时，内核访问该 Socket 的 callback，不需要遍历轮询                                    | epoll 在传递内核与用户空间的消息时使用了内存共享，而不是内存拷贝，这也使得 epoll 的效率比 poll 和 select 更高             |

epoll only makes sense for file descriptors which would normally exhibit blocking behavior on read/write, like pipes and sockets. Normal file descriptors will always either return a result or end-of-file more or less immediately, so epoll wouldn't do anything useful for them.

# Todos

- http://blog.omega-prime.co.uk/2015/09/03/asynchronous-and-non-blocking-io/
