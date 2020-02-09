# Introduction

![](http://cdn4.infoqstatic.com/statics_s1_20160322-0135u2/resource/articles/hadoop-ten-years-interpretation-and-development-forecast/zh/resources/0002.jpg)

# Quick Start

| 组件      | 节点              | 默认端口 | 配置                                                                               | 用途说明                                                         |
| --------- | ----------------- | -------- | ---------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| HDFS      | DataNode          | 50010    | dfs.datanode.address                                                               | datanode 服务端口，用于数据传输                                  |
| HDFS      | DataNode          | 50075    | dfs.datanode.http.address                                                          | http 服务的端口                                                  |
| HDFS      | DataNode          | 50475    | dfs.datanode.https.address                                                         | https 服务的端口                                                 |
| HDFS      | DataNode          | 50020    | dfs.datanode.ipc.address                                                           | ipc 服务的端口                                                   |
| HDFS      | NameNode          | 50070    | dfs.namenode.http-address                                                          | http 服务的端口                                                  |
| HDFS      | NameNode          | 50470    | dfs.namenode.https-address                                                         | https 服务的端口                                                 |
| HDFS      | NameNode          | 8020     | fs.defaultFS                                                                       | 接收 Client 连接的 RPC 端口，用于获取文件系统 metadata 信息      |
| HDFS      | journalnode       | 8485     | dfs.journalnode.rpc-address                                                        | RPC 服务                                                         |
| HDFS      | journalnode       | 8480     | dfs.journalnode.http-address                                                       | HTTP 服务                                                        |
| HDFS      | ZKFC              | 8019     | dfs.ha.zkfc.port                                                                   | ZooKeeper FailoverController，用于 NN HA                         |
| YARN      | ResourceManager   | 8032     | yarn.resourcemanager.address                                                       | RM 的 applications manager(ASM)端口                              |
| YARN      | ResourceManager   | 8030     | yarn.resourcemanager.scheduler.address                                             | scheduler 组件的 IPC 端口                                        |
| YARN      | ResourceManager   | 8031     | yarn.resourcemanager.resource-tracker.address                                      | IPC                                                              |
| YARN      | ResourceManager   | 8033     | yarn.resourcemanager.admin.address                                                 | IPC                                                              |
| YARN      | ResourceManager   | 8088     | yarn.resourcemanager.webapp.address                                                | http 服务端口                                                    |
| YARN      | NodeManager       | 8040     | yarn.nodemanager.localizer.address                                                 | localizer IPC                                                    |
| YARN      | NodeManager       | 8042     | yarn.nodemanager.webapp.address                                                    | http 服务端口                                                    |
| YARN      | NodeManager       | 8041     | yarn.nodemanager.address                                                           | NM 中 container manager 的端口                                   |
| YARN      | JobHistory Server | 10020    | mapreduce.jobhistory.address                                                       | IPC                                                              |
| YARN      | JobHistory Server | 19888    | mapreduce.jobhistory.webapp.address                                                | http 服务端口                                                    |
| HBase     | Master            | 60000    | hbase.master.port                                                                  | IPC                                                              |
| HBase     | Master            | 60010    | hbase.master.info.port                                                             | http 服务端口                                                    |
| HBase     | RegionServer      | 60020    | hbase.regionserver.port                                                            | IPC                                                              |
| HBase     | RegionServer      | 60030    | hbase.regionserver.info.port                                                       | http 服务端口                                                    |
| HBase     | HQuorumPeer       | 2181     | hbase.zookeeper.property.clientPort                                                | HBase-managed ZK mode，使用独立的 ZooKeeper 集群则不会启用该端口 |
| HBase     | HQuorumPeer       | 2888     | hbase.zookeeper.peerport                                                           | HBase-managed ZK mode，使用独立的 ZooKeeper 集群则不会启用该端口 |
| HBase     | HQuorumPeer       | 3888     | hbase.zookeeper.leaderport                                                         | HBase-managed ZK mode，使用独立的 ZooKeeper 集群则不会启用该端口 |
| Hive      | Metastore         | 9083     | /etc/default/hive-metastore 中 export PORT=<port>来更新默认端口                    |                                                                  |
| Hive      | HiveServer        | 10000    | /etc/hive/conf/hive-env.sh 中 export HIVE_SERVER2_THRIFT_PORT=<port>来更新默认端口 |                                                                  |
| ZooKeeper | Server            | 2181     | /etc/zookeeper/conf/zoo.cfg 中 clientPort=<port>                                   | 对客户端提供服务的端口                                           |
| ZooKeeper | Server            | 2888     | /etc/zookeeper/conf/zoo.cfg 中 server.x=[hostname]:nnnnn[:nnnnn]，标蓝部分         | follower 用来连接到 leader，只在 leader 上监听该端口，           |
| ZooKeeper | Server            | 3888     | /etc/zookeeper/conf/zoo.cfg 中 server.x=[hostname]:nnnnn[:nnnnn]，标蓝部分         | 用于 leader 选举的。只在 electionAlg 是 1,2 或 3(默认)时需要，   |

# EcoSystem

云计算由位于网络上的一组服务器把其计算、存储、数据等资源以服务的形式提供给请求者以完成信息处理任务的方法和过程。针对海量文本数据处理,为实现快速 文本处理响应,缩短海量数据为辅助决策提供服务的时间,基于 Hadoop 云计算平台,建立 HDFS 分布式文件系统存储海量文本数据集,通过文本词频利用 MapReduce 原理建立分布式索引,以分布式数据库 HBase 存储关键词索引,并提供实时检索,实现对海量文本数据的分布式并行处理。所 以，Hadoop 是云计算的部分构建。
Hadoop 的生态系统核心组成部分如下图所示:
![](http://img.blog.csdn.net/20160525140324024)

## Docker

如果你本机已经安装好了 Docker 环境，那么可以直接使用如下命令启动某个 Hadoop 实例:

```
docker run -it sequenceiq/hadoop-docker:2.7.1 -p 127.0.0.1:50070:50070 -p 127.0.0.1:18042:8042 -p 127.0.0.1:18088:8088 /etc/bootstrap.sh -bash
```
