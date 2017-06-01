offset 统一表示全局偏移量

pos / position 统一表示单文件中局部偏移量

index 表示某个文件在其文件夹中的下标


本地：

3:49
共发送 4160.000000 K条, 均速 317.848419 K条/s, 耗时 13.088000 秒

4:31
共发送 8840.000000 K条, 均速 376.586853 K条/s, 耗时 23.474001 秒

5:21
共发送 9160.000000 K条, 均速 366.796143 K条/s, 耗时 24.973000 秒

10:28
共发送 9760.000000 K条, 本轮速度 887272.750000, 总均速 607.948181 K条/s,总耗时 16.054001 秒

Ubuntu：

共发送 31200.000000 K条, 均速 225.035156 K条/s, 耗时 138.645004 秒

共发送 31200.000000 K条, 均速 315.990967 K条/s, 耗时 98.737000 秒

512 - true 共发送 8360.000000 K条, 均速 254.327530 K条/s, 耗时 32.870998 秒

512 - false 共发送 9120.000000 K条, 均速 243.154617 K条/s, 耗时 37.507000 秒

1024:5.30 - 共发送 17440.000000 K条, 均速 247.762466 K条/s, 耗时 70.389999 秒

1024:5.31 - 共发送 5550.000000 K条, 本轮速度 355.500000, 总均速 255.078598 K条/s,总耗时 21.757999 秒

1024:6.00 - 共发送 10580.000000 K条, 本轮速度 181.636368, 总均速 261.118530 K条/s,总耗时 40.518002 秒

并发提交：

共发送 30240.000000 K条, 本轮速度 210000.000000, 总均速 631.394348 K条/s,总耗时 47.894001 秒

> 本文记录了近日笔者参与阿里云中间件比赛中，。实际上对于评测环境相较于真实环境下的方案还是由很大的差异，。需要声明的是，LocalMQ 借鉴了 RocketMQ 在 Broker 部分的核心设计思想，最早的源码也是基于 RocketMQ 源码改造而来。另外笔者最近忙于毕业答辩，本文很多内容可能存在谬误，请读者批评指正。

# LocalMQ：从零构建类 RocketMQ 高性能消息队列

比赛还是挺有意思的，虽然成绩一直不理想，但是看着与自己相比却在不断地提高。

在编写的过程中，笔者发现对于执行流的优化、避免重复计算与额外变量、选择使用合适的并发策略都会对结果造成极大的影响，笔者从 SpinLock 切换到 重入锁之后，本地测试 TPS 增加了约 5%。

- 优化代码：

- 异步 IO，顺序 Flush。笔者发现，如果多个线程进行并发 Flush 操作，反而不如单线程进行顺序 Flush

- 并发计算优化，将所有的耗时计算放到可以并发的 Producer 中

- 使用合理的锁，重入锁相较于自旋锁有近 5 倍的 TPS 提升

- 并发消息提交

评测环境与真实环境的差异还是蛮大的，评测环境版本的 EmbeddedMessageQueue 中我们可以在各 Producer 线程中单独将消息持久化入文件中，而在 LocalMessageQueue 中，我们是将消息统一写入 MessageStore 中，然后又 PostPutMessageService 进行二次处理。

- 混合处理与标记位：

- 持久化存储到磁盘的时机：

- 添加消息的后处理：

- 未考虑断续拉取的情况：EmbeddedMessageQueue 中是假设 Consumer 能够单次处理完某个 BucketQueue 中的单个 Partition 的全部消息，因此记录其处理值时也仅是记录了文件级别的位移，如果存在某次是仅拉取了单个 Partition 中部分内容，则下次的起始拉取点还是下个文件首。

# 设计概要

## 数据存储

## Producer

## Consumer

## 性能优化

### 代码级别优化

ArrayList -> LinkedList
ConcurrentHashMap -> HashMap



笔者在实现 LocalMQ 的过程中感触最深的就是实现相同功能的不同代码在性能上的差异可能会很大。

- 并发计算

- 避免额外空间申请与垃圾回收
```java

```
```$xslt
[2017-06-01 12:13:01,081] [CMS-concurrent-sweep: 1.182/2.457 secs] [Times: user=6.48 sys=0.17, real=2.45 secs] 
[2017-06-01 12:13:01,081] [CMS-concurrent-reset-start]
[2017-06-01 12:13:01,087] [CMS-concurrent-reset: 0.006/0.006 secs] [Times: user=0.01 sys=0.00, real=0.01 secs] 
[2017-06-01 12:13:01,455] [GC (Allocation Failure) [ParNew: 306688K->34047K(306688K), 0.2351621 secs] 1177351K->968629K(2587392K), 0.2352775 secs] [Times: user=0.83 sys=0.00, real=0.24 secs] 
[2017-06-01 12:13:02,076] [GC (Allocation Failure) [ParNew: 306687K->34047K(306688K), 0.2452635 secs] 1241269K->1029647K(2587392K), 0.2453580 secs] [Times: user=0.85 sys=0.01, real=0.25 secs] 
[2017-06-01 12:13:02,518] [GC (Allocation Failure) [ParNew: 306687K->34047K(306688K), 0.2217404 secs] 1302287K->1090458K(2587392K), 0.2218330 secs] [Times: user=0.74 sys=0.00, real=0.22 secs] 
[2017-06-01 12:13:03,202] [GC (Allocation Failure) [ParNew: 306687K->34046K(306688K), 0.2447910 secs] 1363098K->1154451K(2587392K), 0.2448822 secs] [Times: user=0.78 sys=0.01, real=0.25 secs] 
[2017-06-01 12:13:03,948] [GC (Allocation Failure) [ParNew: 306686K->34046K(306688K), 0.2024332 secs] 1427091K->1211933K(2587392K), 0.2025556 secs] [Times: user=0.72 sys=0.01, real=0.20 secs] 
[2017-06-01 12:13:04,370] [GC (Allocation Failure) [ParNew: 306686K->34046K(306688K), 0.2289574 secs] 1484573K->1265775K(2587392K), 0.2290781 secs] [Times: user=0.75 sys=0.01, real=0.23 secs] 
[2017-06-01 12:13:05,065] [GC (Allocation Failure) [ParNew: 306686K->34046K(306688K), 0.1825507 secs] 1538415K->1310964K(2587392K), 0.1826916 secs] [Times: user=0.65 sys=0.01, real=0.18 secs] 
[2017-06-01 12:13:05,457] [GC (Allocation Failure) [ParNew: 306686K->34046K(306688K), 0.2022820 secs] 1583604K->1351946K(2587392K), 0.2024721 secs] [Times: user=0.61 sys=0.01, real=0.20 secs] 
[2017-06-01 12:13:05,911] [GC (Allocation Failure) [ParNew: 306686K->34046K(306688K), 0.1124183 secs] 1624586K->1370628K(2587392K), 0.1125322 secs] [Times: user=0.38 sys=0.01, real=0.11 secs] 
```
```
[2017-06-01 12:13:21,802] INFO: 构建耗时占比：0.471270，发送耗时占比：0.428567，持久化耗时占比：0.100163
```

```$xslt
INFO: 构建耗时占比：0.275170，发送耗时占比：0.573520，持久化耗时占比：0.151309
```


### 异步 IO

### 并发控制

# EmbeddedMessageQueue 

```$xslt
mvn clean package -U assembly:assembly -Dmaven.test.skip=true

java -Xmx2048m -Xms2048m  -cp open-messaging-wx.demo-1.0.jar  wx.demo.benchmark.ProducerBenchmark
```

# LocalMessageQueue