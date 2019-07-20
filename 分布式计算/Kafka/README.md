![](https://www.confluent.io/wp-content/uploads/streaming_platform_rev.png)

# Kafka

Kafka 它本质上是一个消息系统，它提供了常用的消息系统的功能集，但是它的设计更加独特，原本开发自 LinkedIn，用作 LinkedIn 的活动流(Activity Stream)和运营数据处理管道(Pipeline)的基础。现在它已被多家不同类型的公司 作为多种类型的数据管道和消息系统使用。

![](http://images0.cnblogs.com/blog2015/666745/201505/261159103182564.png)

首先，Kafka 可以应用于消息系统，比如，当下较为热门的消息推送，这些消息推送系统的消息源，可以使用 Kafka 作为系统的核心组建来完成消息的生产 和消息的消费。然后是网站的行迹，我们可以将企业的 Portal，用户的操作记录等信息发送到 Kafka 中，按照实际业务需求，可以进行实时监控，或者做离线处理等。最后，一个是日志收集，类似于 Flume 套件这样的日志收集系统，但 Kafka 的设计架构采用 push/pull，适合异构集群，Kafka 可以批量提交消息，对 Producer 来说，在性能方面基本上是无消耗的，而在 Consumer 端中，我们可以使用 HDFS 这类的分布式文件存储系统进行存储。

# 特性

# 组件

Kafka 是一种分布式的，基于发布/订阅的消息系统。主要设计目标如下：

- 以时间复杂度为 O(1)的方式提供消息持久化能力，即使对 TB 级以上数据也能保证常数时间复杂度的访问性能。
- 高吞吐率。即使在非常廉价的商用机器上也能做到单机支持每秒 100K 条以上消息的传输。
- 支持 Kafka Server 间的消息分区，及分布式消费，同时保证每个 Partition 内的消息顺序传输。
- 同时支持离线数据处理和实时数据处理。
- Scale out：支持在线水平扩展。

**关键概念**

| Concepts    | Function                                                                                                                                                                                                                                                                                                                                      |
| ----------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Topic       | 用于划分 Message 的逻辑概念，一个 Topic 可以分布在多个 Broker 上。                                                                                                                                                                                                                                                                            |
| Partition   | 是 Kafka 中横向扩展和一切并行化的基础，每个 Topic 都至少被切分为 1 个 Partition。                                                                                                                                                                                                                                                             |
| Offset      | 消息在 Partition 中的编号，编号顺序不跨 Partition(在 Partition 内有序)。                                                                                                                                                                                                                                                                      |
| Consumer    | 用于从 Broker 中取出/消费 Message。                                                                                                                                                                                                                                                                                                           |
| Producer    | 用于往 Broker 中发送/生产 Message。                                                                                                                                                                                                                                                                                                           |
| Replication | Kafka 支持以 Partition 为单位对 Message 进行冗余备份，每个 Partition 都可以配置至少 1 个 Replication(当仅 1 个 Replication 时即仅该 Partition 本身)。                                                                                                                                                                                         |
| Leader      | 每个 Replication 集合中的 Partition 都会选出一个唯一的 Leader，所有的读写请求都由 Leader 处理。其他 Replicas 从 Leader 处把数据更新同步到本地。                                                                                                                                                                                               |
| Broker      | Kafka 中使用 Broker 来接受 Producer 和 Consumer 的请求，并把 Message 持久化到本地磁盘。每个 Cluster 当中会选举出一个 Broker 来担任 Controller，负责处理 Partition 的 Leader 选举，协调 Partition 迁移等工作。                                                                                                                                 |
| ISR         | In-Sync Replica,是 Replicas 的一个子集，表示目前 Alive 且与 Leader 能够“Catch-up”的 Replicas 集合。由于读写都是首先落到 Leader 上，所以一般来说通过同步机制从 Leader 上拉取数据的 Replica 都会和 Leader 有一些延迟(包括了延迟时间和延迟条数两个维度)，任意一个超过阈值都会把该 Replica 踢出 ISR。每个 Leader Partition 都有它自己独立的 ISR。 |

**设计思想**

| Concepts           | Function                                                                                                                                                                                                                             |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Consumergroup      | 各个 consumer 可以组成一个组，每个消息只能被组中的一个 consumer 消费，如果一个消息可以被多个 consumer 消费的话，那么这些 consumer 必须在不同的组。                                                                                   |
| 消息状态           | 在 Kafka 中，消息的状态被保存在 consumer 中，broker 不会关心哪个消息被消费了被谁消费了，只记录一个 offset 值(指向 partition 中下一个要被消费的消息位置)，这就意味着如果 consumer 处理不好的话，broker 上的一个消息可能会被消费多次。 |
| 消息持久化         | Kafka 中会把消息持久化到本地文件系统中，并且保持极高的效率。                                                                                                                                                                         |
| 消息有效期         | Kafka 会长久保留其中的消息，以便 consumer 可以多次消费，当然其中很多细节是可配置的。                                                                                                                                                 |
| 批量发送           | Kafka 支持以消息集合为单位进行批量发送，以提高 push 效率。                                                                                                                                                                           |
| push-and-pull      | Kafka 中的 Producer 和 consumer 采用的是 push-and-pull 模式，即 Producer 只管向 broker push 消息，consumer 只管从 broker pull 消息，两者对消息的生产和消费是异步的。                                                                 |
| Broker 之间的关系  | 不是主从关系，各个 broker 在集群中地位一样，我们可以随意的增加或删除任何一个 broker 节点。                                                                                                                                           |
| 负载均衡           | Kafka 提供了一个 metadata API 来管理 broker 之间的负载(对 Kafka0.8.x 而言，对于 0.7.x 主要靠 zookeeper 来实现负载均衡)。                                                                                                             |
| 同步异步           | Producer 采用异步 push 方式，极大提高 Kafka 系统的吞吐率(可以通过参数控制是采用同步还是异步方式)。                                                                                                                                   |
| 分区机制 partition | Kafka 的 broker 端支持消息分区，Producer 可以决定把消息发到哪个分区，在一个分区中消息的顺序就是 Producer 发送消息的顺序，一个主题中可以有多个分区，具体分区的数量是可配置的。分区的意义很重大，后面的内容会逐渐体现。                |

# 特性与场景

## Use Cases | 使用场景

### Message Broker | 用于异构服务之间的消息传递

### Statistic Analysis | 统计分析

活动流数据是几乎所有站点在对其网站使用情况做报表时都要用到的数据中最常规的部分。活动数据包括页面访问量(Page View)、被查看内容方面的信息以及搜索情况等内容。这种数据通常的处理方式是先把各种活动以日志的形式写入某种文件，然后周期性地对这些文件进行统计分析。运营数据指的是服务器的性能数据(CPU、IO 使用率、请求时间、服务日志等等数据)。运营数据的统计方法种类繁多。

近年来，活动和运营数据处理已经成为了网站软件产品特性中一个至关重要的组成部分，这就需要一套稍微更加复杂的基础设施对其提供支持。

### Stream Processing | 流计算
