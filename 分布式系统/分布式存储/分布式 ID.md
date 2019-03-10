![default](https://i.postimg.cc/V6m3yh19/image.png)

# 分布式 ID

分布式节点各自独立生成全局唯一的 ID，有 Twitter 的 snowflake 机制、zookeeper 机制、微信的序列号方式以及 sequence 机制，不同的算法适应不同的场景，但我认为方法虽然很多，但原理只有两个，一种唯一性来自于节点本身的唯一标识，另一种唯一性来自于单点的唯一性。

1. 节点本身有一个唯一标识，依赖节点自身标识实现生成 ID 的全局唯一性。比如 zookeeper，每个 zookeeper 节点有一个预先设定好的 nodeId，nodeId 必须是一个正整数，zookeeper 节点生成的 id 都是本节点 nodeId 的同余类(比如集群数量是 5，当前节点的 nodeId=1 时，生成的 id=5\*n + 1)，由于本节点生成的 id 递增，不同节点生成的 id 集合互不相交，因此保证了 id 的全局唯一。但是这种场景下节点需要持久化当前偏移量，在崩溃恢复后保证 id 的唯一性，此外如果集群数量发生变化，可能导致 id 重复，需要额外的代价做到集群数量增加。
2. 节点本身无状态，由中心节点生成 id，这种方式对高可用和性能的挑战很高，但是可以生成全区唯一并且有序的序列，客户端不需要记录任何信息，每次需要 id 就向中心申请即可。微信的序列号就是这么做的，生成的 id 严格有序，并且做到高可用和高吞吐，具体参考http://www.infoq.com/cn/articles/wechat-serial-number-generator-architecture。

# UUID

什么是 UUID？

UUID 是 Universally Unique Identifier 的缩写，它是在一定的范围内（从特定的名字空间到全球）唯一的机器生成的标识符。UUID 具有以下涵义：

经由一定的算法机器生成
为了保证 UUID 的唯一性，规范定义了包括网卡 MAC 地址、时间戳、名字空间（Namespace）、随机或伪随机数、时序等元素，以及从这些元素生成 UUID 的算法。UUID 的复杂特性在保证了其唯一性的同时，意味着只能由计算机生成。
非人工指定，非人工识别
UUID 是不能人工指定的，除非你冒着 UUID 重复的风险。UUID 的复杂性决定了“一般人“不能直接从一个 UUID 知道哪个对象和它关联。
在特定的范围内重复的可能性极小
UUID 的生成规范定义的算法主要目的就是要保证其唯一性。但这个唯一性是有限的，只在特定的范围内才能得到保证，这和 UUID 的类型有关（参见 UUID 的版本）。

UUID 是 16 字节 128 位长的数字，通常以 36 字节的字符串表示，示例如下：

3F2504E0-4F89-11D3-9A0C-0305E82C3301

其中的字母是 16 进制表示，大小写无关。

GUID（Globally Unique Identifier）是 UUID 的别名；但在实际应用中，GUID 通常是指微软实现的 UUID。

UUID 的版本

UUID 具有多个版本，每个版本的算法不同，应用范围也不同。

首先是一个特例－－Nil UUID－－通常我们不会用到它，它是由全为 0 的数字组成，如下：

00000000-0000-0000-0000-000000000000

UUID Version 1：基于时间的 UUID

基于时间的 UUID 通过计算当前时间戳、随机数和机器 MAC 地址得到。由于在算法中使用了 MAC 地址，这个版本的 UUID 可以保证在全球范围的唯一性。但与此同时，使用 MAC 地址会带来安全性问题，这就是这个版本 UUID 受到批评的地方。如果应用只是在局域网中使用，也可以使用退化的算法，以 IP 地址来代替 MAC 地址－－Java 的 UUID 往往是这样实现的（当然也考虑了获取 MAC 的难度）。

UUID Version 2：DCE 安全的 UUID

DCE（Distributed Computing Environment）安全的 UUID 和基于时间的 UUID 算法相同，但会把时间戳的前 4 位置换为 POSIX 的 UID 或 GID。这个版本的 UUID 在实际中较少用到。

UUID Version 3：基于名字的 UUID（MD5）

基于名字的 UUID 通过计算名字和名字空间的 MD5 散列值得到。这个版本的 UUID 保证了：相同名字空间中不同名字生成的 UUID 的唯一性；不同名字空间中的 UUID 的唯一性；相同名字空间中相同名字的 UUID 重复生成是相同的。

UUID Version 4：随机 UUID

根据随机数，或者伪随机数生成 UUID。这种 UUID 产生重复的概率是可以计算出来的，但随机的东西就像是买彩票：你指望它发财是不可能的，但狗屎运通常会在不经意中到来。

UUID Version 5：基于名字的 UUID（SHA1）

和版本 3 的 UUID 算法类似，只是散列值计算使用 SHA1（Secure Hash Algorithm 1）算法。

UUID 的应用

从 UUID 的不同版本可以看出，Version 1/2 适合应用于分布式计算环境下，具有高度的唯一性；Version 3/5 适合于一定范围内名字唯一，且需要或可能会重复生成 UUID 的环境下；至于 Version 4，我个人的建议是最好不用（虽然它是最简单最方便的）。

通常我们建议使用 UUID 来标识对象或持久化数据，但以下情况最好不使用 UUID：

映射类型的对象。比如只有代码及名称的代码表。
人工维护的非系统生成对象。比如系统中的部分基础数据。
对于具有名称不可重复的自然特性的对象，最好使用 Version 3/5 的 UUID。比如系统中的用户。如果用户的 UUID 是 Version 1 的，如果你不小心删除了再重建用户，你会发现人还是那个人，用户已经不是那个用户了。（虽然标记为删除状态也是一种解决方案，但会带来实现上的复杂性。）

UUID 生成器

```sh
[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}
```

# 基于数据库的 ID 生成

在单数据源情况下，基于原理 2 生成 id，此时数据库就是中心节点，所有序列号由中心节点派发；而在多数据源和单元化场景下，结合了原理 1 和原理 2，单元化是多数据源的特例，支持多数据源就能方便实现单元化支持。

这种情况下一个序列只存储于一张 sequence 表，使用 SequenceDao 进行表的读写。一张 sequence 表可以存储多个序列，不同的序列用名字(name)字段区分，所以一般而言一个应用一个 sequence 表就够了，甚至多个应用可以共享一张 sequence 表。通过这种单点数据源的方式，可以实现序列全局唯一，由于数据库知道目前派发出去的最大 id 号，后续的请求，只要在现在基础上递增即可。现在应用申请一个 id，需要用乐观锁（CAS）的方式更新序列的值，但是每个 id 都要乐观锁更新 db 实在太慢，因此为了加速 id 分配，一次 CAS 操作批量分配 id，分配 id 的数量就是 sequence 的内步长，默认的内步长是 1000，这样可以降低 1000 倍的数据库访问，不过有得必有失，这种方式生成的序列就不能保证全局有序了，并且在应用重启的时候，将会重新申请一段 id。sequence 所用的数据库表结构如下：

```sql
-- ----------------------------
--  Table structure for `sequence`
-- ----------------------------
DROP TABLE IF EXISTS `sequence`;
CREATE TABLE `sequence` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id',
  `name` varchar(64) NOT NULL COMMENT 'sequence name',
  `value` bigint(20) NOT NULL COMMENT 'sequence current value',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='sequence'
;
```

sequence 表的核心字段就两个：name 和 value，name 就是当前序列的名称，value 就是当前 sequence 已经分配出去的 id 最大值。多数据源的场景除了能够自适应后面的单元化场景之外，还可以避免单点故障，即一个 sequence 表不可用情况下，sequence 能够用其他数据源继续提供 id 生成服务。

就是基于之前的原理 1，即为每个数据源分配一个序号，数据源根据自己的序号在序列空间中独占一组序列号，原理如下图：

![image](https://user-images.githubusercontent.com/5803001/50880267-d1044780-1418-11e9-826b-f368f440b151.png)
