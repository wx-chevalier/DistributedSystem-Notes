# Pod

按照容器的设计理念，每个容器只运行单个进程。而要想实现多个container被绑定在一起进行管理的需求。我们需要一种高级别的概念来实现这个。在kubernetes中，这就是Pod。在Pod里面，container之间可以共享网络（IP/Port）、共享存储（Volume）、共享Hostname。

另外，Pod可以理解成一个”逻辑主机”，它与非容器领域的物理主机或者VM有着类似的行为。在同一个Pod运行的进程就像在同一物理主机或VM上运行的进程一样。只是这些进程被单独的放到单个container内。

# 链接

- https://mp.weixin.qq.com/s/Vrgff_qfKWPFmRPb_LVMmQ