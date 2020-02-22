[toc]

# 主从复制

存储数据库副本的每个节点称为 副本（replica）。当存在多个副本时，会不可避免的出现一个问题：如何确保所有数据都落在了所有的副本上？每一次向数据库的写入操作都需要传播到所有副本上，否则副本就会包含不一样的数据。最常见的解决方案被称为基于领导者的复制（leader-based replication）（也称主动/被动（active/passive）或主/从（master/slave）复制），它的工作原理如下：

- 副本之一被指定为 领导者（leader），也称为 主库（master|primary）。当客户端要向数据库写入时，它必须将请求发送给领导者，领导者会将新数据写入其本地存储。

- 其他副本被称为追随者（followers），亦称为只读副本（read replicas），从库（slaves），备库（sencondaries），热备（hot-standby）。每当领导者将新数据写入本地存储时，它也会将数据变更发送给所有的追随者，称之为复制日志（replication log）记录或变更流（change stream）。每个跟随者从领导者拉取日志，并相应更新其本地数据库副本，方法是按照领导者处理的相同顺序应用所有写入。

- 当客户想要从数据库中读取数据时，它可以向领导者或追随者查询但只有领导者才能接受写操作（从客户端的角度来看从库都是只读的）。

![基于领导者(主-从)的复制](https://s2.ax1x.com/2020/02/08/1WwR1O.png)

这种复制模式是许多关系数据库的内置功能，如 PostgreSQL（从 9.0 版本开始），MySQL，Oracle Data Guard 和 SQL Server 的 AlwaysOn 可用性组它也被用于一些非关系数据库，包括 MongoDB，RethinkDB 和 Espresso 最后，基于领导者的复制并不仅限于数据库：像 Kafka 和 RabbitMQ 高可用队列这样的分布式消息代理也使用它某些网络文件系统，例如 DRBD 这样的块复制设备也与之类似。
