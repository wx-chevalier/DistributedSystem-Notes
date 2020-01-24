# 分布式存储

分布式存储系统，广义上来讲，将文件存储抽象化，之前提到的块存储和对象存储都建立在这个系统之上。从某些角度来讲，存储系统相当于中间件，建立在底层的 SATA 或者 SSD 磁盘之上，而服务于上层的块存储。

# 挑战

分布式存储系统（Distributed Storage System）的核心不外乎两点：分片策略（Sharding Strategy）与元数据存储（Metadata Storage）机制，我们在保证存储系统弹性可扩展（Elastic Scalability）的同时需要保证系统的透明性（Transpant）与一致性（Consistent）。

在数据库系统中我们熟知的分片策略，譬如 Cobar 之于 MySQL，Twemproxy 之于 Redis，都是典型的静态分片策略，它们都难以无感扩展。
