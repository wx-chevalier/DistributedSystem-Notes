# 分片策略

分片是一种数据库分区策略，可将数据集分成较小的部分，并将它们存储在不同的物理节点中。分片单元（Sharding Unit）是数据移动与平衡的最小单元，集群中的每个物理节点都存储多个分片单元。

在数据库系统中我们熟知的分片策略，譬如 Cobar 之于 MySQL，Twemproxy 之于 Redis，都是典型的静态分片策略，它们都难以无感扩展。我们常用的两个分片策略是基于范围的分片（Range-based Sharding）与基于 Hash 的分片（Hash-based Sharding），不同的系统会选用不同的分片策略。典型的分布式分片系统就是 HDFS：

![HDFS](https://s2.ax1x.com/2020/01/25/1eu8hT.md.png)
