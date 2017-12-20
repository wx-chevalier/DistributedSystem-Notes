
# Redis Cluster

## Reference
- [深入浅出Redis Cluster原理](http://mp.weixin.qq.com/s?__biz=MzA3MzYwNjQ3NA==&mid=2651296996&idx=2&sn=5f4811d73e74e2a63b1cb0d3d532862a)

Redis 3.0集群采用了P2P的模式，完全去中心化。Redis把所有的Key分成了16384个slot，每个Redis实例负责其中一部分slot。集群中的所有信息（节点、端口、slot等），都通过节点之间定期的数据交换而更新。

Redis客户端在任意一个Redis实例发出请求，如果所需数据不在该实例中，通过重定向命令引导客户端访问所需的实例。

Redis 3.0集群的工作流程如图4所示。

![img](http://mmbiz.qpic.cn/mmbiz/tzia4bcY5HEKxeYTFdFSwaLu6W5SRXboVsbPPVPFxWHX3Qs38CPS8q4TxcKC6emHlDIq0ZvsopSxq3eiajCOFlWQ/640?wx_fmt=png&wxfrom=5&wx_lazy=1)

*图4Redis 3.0集群的工作流程图*

如图4所示Redis集群内的机器定期交换数据，工作流程如下。

（1）      Redis客户端在Redis2实例上访问某个数据。

（2）      在Redis2内发现这个数据是在Redis3这个实例中，给Redis客户端发送一个重定向的命令。

（3）      Redis客户端收到重定向命令后，访问Redis3实例获取所需的数据。

Redis 3.0的集群方案有以下两个问题。

- 一个Redis实例具备了“数据存储”和“路由重定向”，完全去中心化的设计。这带来的好处是部署非常简单，直接部署Redis就行，不像Codis有那么多的组件和依赖。但带来的问题是很难对业务进行无痛的升级，如果哪天Redis集群出了什么严重的Bug，就只能回滚整个Redis集群。
- 对协议进行了较大的修改，对应的Redis客户端也需要升级。升级Redis客户端后谁能确保没有Bug？而且对于线上已经大规模运行的业务，升级代码中的Redis客户端也是一个很麻烦的事情。

综合上面所述的两个问题，Redis 3.0集群在业界并没有被大规模使用。