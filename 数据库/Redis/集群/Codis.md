![default](https://user-images.githubusercontent.com/5803001/45228854-de88b400-b2f6-11e8-9ab0-d393ed19f21f.png)

Twemproxy 不能平滑增加 Redis 实例的问题带来了很大的不便，于是豌豆荚自主研发了 Codis，一个支持平滑增加 Redis 实例的 Redis 代理软件，其基于 Go 和 C 语言开发，并于 2014 年 11 月在 GitHub 上开源。

Codis 包含下面 4 个部分。

- [Codis](#codis)
- [Quick Start](#quick-start)
- [HA](#ha)

Codis 的架构如图 3 所示。

![img](http://mmbiz.qpic.cn/mmbiz/tzia4bcY5HEKxeYTFdFSwaLu6W5SRXboVLibV5KT1dt17aHpjNKsAHhnemHSEuB4dkyXqpp1LrliaRW2eaNMicjTGg/640?wx_fmt=png&wxfrom=5&wx_lazy=1)

_图 3Codis 的架构图_

在图 3 的 Codis 的架构图中，Codis 引入了 Redis Server Group，其通过指定一个主 CodisRedis 和一个或多个从 CodisRedis，实现了 Redis 集群的高可用。当一个主 CodisRedis 挂掉时，Codis 不会自动把一个从 CodisRedis 提升为主 CodisRedis，这涉及数据的一致性问题(Redis 本身的数据同步是采用主从异步复制，当数据在主 CodisRedis 写入成功时，从 CodisRedis 是否已读入这个数据是没法保证的)，需要管理员在管理界面上手动把从 CodisRedis 提升为主 CodisRedis。

如果觉得麻烦，豌豆荚也提供了一个工具 Codis-ha，这个工具会在检测到主 CodisRedis 挂掉的时候将其下线并提升一个从 CodisRedis 为主 CodisRedis。

Codis 中采用预分片的形式，启动的时候就创建了 1024 个 slot，1 个 slot 相当于 1 个箱子，每个箱子有固定的编号，范围是 1~1024。slot 这个箱子用作存放 Key，至于 Key 存放到哪个箱子，可以通过算法“crc32(key)%1024”获得一个数字，这个数字的范围一定是 1~1024 之间，Key 就放到这个数字对应的 slot。例如，如果某个 Key 通过算法“crc32(key)%1024”得到的数字是 5，就放到编码为 5 的 slot(箱子)。1 个 slot 只能放 1 个 Redis Server Group，不能把 1 个 slot 放到多个 Redis Server Group 中。1 个 Redis Server Group 最少可以存放 1 个 slot，最大可以存放 1024 个 slot。因此，Codis 中最多可以指定 1024 个 Redis Server Group。

Codis 最大的优势在于支持平滑增加(减少)Redis Server Group(Redis 实例)，能安全、透明地迁移数据，这也是 Codis 有别于 Twemproxy 等静态分布式 Redis 解决方案的地方。Codis 增加了 Redis Server Group 后，就牵涉到 slot 的迁移问题。例如，系统有两个 Redis Server Group，Redis Server Group 和 slot 的对应关系如下。

| Redis Server Group | slot     |
| ------------------ | -------- |
| 1                  | 1~500    |
| 2                  | 501~1024 |

当增加了一个 Redis Server Group，slot 就要重新分配了。Codis 分配 slot 有两种方法。

第一种：通过 Codis 管理工具 Codisconfig 手动重新分配，指定每个 Redis Server Group 所对应的 slot 的范围，例如可以指定 Redis Server Group 和 slot 的新的对应关系如下。

| Redis Server Group | slot     |
| ------------------ | -------- |
| 1                  | 1~500    |
| 2                  | 501~700  |
| 3                  | 701~1024 |

第二种：通过 Codis 管理工具 Codisconfig 的 rebalance 功能，会自动根据每个 Redis Server Group 的内存对 slot 进行迁移，以实现数据的均衡。

# Codis

Codis Twemproxy Redis Cluster
resharding without restarting cluster Yes No Yes
pipeline Yes Yes No
hash tags for multi-key operations Yes Yes Yes
multi-key operations while resharding Yes - No(details)
Redis clients supporting Any clients Any clients Clients have to support cluster protocol

# Quick Start

# HA
