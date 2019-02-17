![default](https://user-images.githubusercontent.com/5803001/45228854-de88b400-b2f6-11e8-9ab0-d393ed19f21f.png)

# Redis Cluster

Redis 3.0 集群采用了 P2P 的模式，完全去中心化。Redis 把所有的 Key 分成了 16384 个 slot，每个 Redis 实例负责其中一部分 slot。集群中的所有信息(节点、端口、slot 等)，都通过节点之间定期的数据交换而更新。

Redis 客户端在任意一个 Redis 实例发出请求，如果所需数据不在该实例中，通过重定向命令引导客户端访问所需的实例。

Redis 3.0 集群的工作流程如图 4 所示。

![img](http://mmbiz.qpic.cn/mmbiz/tzia4bcY5HEKxeYTFdFSwaLu6W5SRXboVsbPPVPFxWHX3Qs38CPS8q4TxcKC6emHlDIq0ZvsopSxq3eiajCOFlWQ/640?wx_fmt=png&wxfrom=5&wx_lazy=1)

_图 4Redis 3.0 集群的工作流程图_

如图 4 所示 Redis 集群内的机器定期交换数据，工作流程如下。

(1) Redis 客户端在 Redis2 实例上访问某个数据。

(2) 在 Redis2 内发现这个数据是在 Redis3 这个实例中，给 Redis 客户端发送一个重定向的命令。

(3) Redis 客户端收到重定向命令后，访问 Redis3 实例获取所需的数据。

Redis 3.0 的集群方案有以下两个问题。

- 一个 Redis 实例具备了“数据存储”和“路由重定向”，完全去中心化的设计。这带来的好处是部署非常简单，直接部署 Redis 就行，不像 Codis 有那么多的组件和依赖。但带来的问题是很难对业务进行无痛的升级，如果哪天 Redis 集群出了什么严重的 Bug，就只能回滚整个 Redis 集群。
- 对协议进行了较大的修改，对应的 Redis 客户端也需要升级。升级 Redis 客户端后谁能确保没有 Bug？而且对于线上已经大规模运行的业务，升级代码中的 Redis 客户端也是一个很麻烦的事情。

综合上面所述的两个问题，Redis 3.0 集群在业界并没有被大规模使用。
