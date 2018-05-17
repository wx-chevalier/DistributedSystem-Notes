Twemproxy不能平滑增加Redis实例的问题带来了很大的不便，于是豌豆荚自主研发了Codis，一个支持平滑增加Redis实例的Redis代理软件，其基于Go和C语言开发，并于2014年11月在GitHub上开源。

Codis包含下面4个部分。

- [Codis](#codis)
- [Quick Start](#quick-start)
- [HA](#ha)

Codis的架构如图3所示。

![img](http://mmbiz.qpic.cn/mmbiz/tzia4bcY5HEKxeYTFdFSwaLu6W5SRXboVLibV5KT1dt17aHpjNKsAHhnemHSEuB4dkyXqpp1LrliaRW2eaNMicjTGg/640?wx_fmt=png&wxfrom=5&wx_lazy=1)

*图3Codis的架构图*

在图3的Codis的架构图中，Codis引入了Redis Server Group，其通过指定一个主CodisRedis和一个或多个从CodisRedis，实现了Redis集群的高可用。当一个主CodisRedis挂掉时，Codis不会自动把一个从CodisRedis提升为主CodisRedis，这涉及数据的一致性问题(Redis本身的数据同步是采用主从异步复制，当数据在主CodisRedis写入成功时，从CodisRedis是否已读入这个数据是没法保证的)，需要管理员在管理界面上手动把从CodisRedis提升为主CodisRedis。

如果觉得麻烦，豌豆荚也提供了一个工具Codis-ha，这个工具会在检测到主CodisRedis挂掉的时候将其下线并提升一个从CodisRedis为主CodisRedis。

Codis中采用预分片的形式，启动的时候就创建了1024个slot，1个slot相当于1个箱子，每个箱子有固定的编号，范围是1~1024。slot这个箱子用作存放Key，至于Key存放到哪个箱子，可以通过算法“crc32(key)%1024”获得一个数字，这个数字的范围一定是1~1024之间，Key就放到这个数字对应的slot。例如，如果某个Key通过算法“crc32(key)%1024”得到的数字是5，就放到编码为5的slot(箱子)。1个slot只能放1个Redis Server Group，不能把1个slot放到多个Redis Server Group中。1个Redis Server Group最少可以存放1个slot，最大可以存放1024个slot。因此，Codis中最多可以指定1024个Redis Server Group。

Codis最大的优势在于支持平滑增加(减少)Redis Server Group(Redis实例)，能安全、透明地迁移数据，这也是Codis 有别于Twemproxy等静态分布式 Redis 解决方案的地方。Codis增加了Redis Server Group后，就牵涉到slot的迁移问题。例如，系统有两个Redis Server Group，Redis Server Group和slot的对应关系如下。

| Redis Server Group | slot     |
| ------------------ | -------- |
| 1                  | 1~500    |
| 2                  | 501~1024 |

当增加了一个Redis Server Group，slot就要重新分配了。Codis分配slot有两种方法。

第一种：通过Codis管理工具Codisconfig手动重新分配，指定每个Redis Server Group所对应的slot的范围，例如可以指定Redis Server Group和slot的新的对应关系如下。

| Redis Server Group | slot     |
| ------------------ | -------- |
| 1                  | 1~500    |
| 2                  | 501~700  |
| 3                  | 701~1024 |

第二种：通过Codis管理工具Codisconfig的rebalance功能，会自动根据每个Redis Server Group的内存对slot进行迁移，以实现数据的均衡。

# Codis
Codis	Twemproxy	Redis Cluster
resharding without restarting cluster	Yes	No	Yes
pipeline	Yes	Yes	No
hash tags for multi-key operations	Yes	Yes	Yes
multi-key operations while resharding	Yes	-	No(details)
Redis clients supporting	Any clients	Any clients	Clients have to support cluster protocol
# Quick Start

# HA
