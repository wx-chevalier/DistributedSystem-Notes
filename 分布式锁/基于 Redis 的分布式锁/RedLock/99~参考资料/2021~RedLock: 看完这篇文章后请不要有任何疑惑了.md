# RedLock: 看完这篇文章后请不要有任何疑惑了

后台经常会有小伙伴咨询 RedLock 相关问题，笔者在此再来一篇文章剖析一下 RedLock，希望此文能解决你对它所有的疑惑和误解。

## 为什么需要 RedLock

这一点很好理解，因为普通的分布式锁算法在加锁时它的 KEY 只会存在于某一个 Redis Master 实例以及它的 slave 上（假如有 slave 的话， 即使 cluster 集群模式，也是一样的。因为一个 KEY 只会属于一个 slot，一个 slot 只会属于一个 Redis 节点），如下图所示（图中虚线表示 cluster 中 gossip 协议交互路径）：
![image.png](https://a.perfma.net/img/2410473)

因为它只会存在于某一个 Redis Master 上，而 Redis 又不是基于 CP 模型的。那么就会有很大概率存在锁丢失的情况。以如下场景为例：
1、线程 T1 在 M1 中加锁成功。
2、M1 出现故障，但是由于主从同步延迟问题，加锁的 KEY 并没有同步到 S1 上。
3、S1 升级为 Master 节点。
4、另一个线程 T2 在 S1 上也加锁成功，从而导致线程 T1 和 T2 都获取到了分布式锁。

而 RedLock 方法就是根除普通基于 Redis 分布式锁而生的（无论是主从模式、sentinel 模式还是 cluster 模式）！官方把 RedLock 方法当作使用 Redis 实现分布式锁的规范算法，并认为这种实现比普通的单实例或者基于 Redis Cluster 的实现更安全。

## RedLock 定义

首先，我们要掌握 RedLock 的第一步就是了解它的定义。这一点，官方网站肯定是最权威的。接下来的这段文字摘自 http://redis.cn/topics/distlock.html
在 Redis 的分布式环境中，我们假设有 N 个 Redis Master。这些节点完全互相独立，不存在主从复制或者其他集群协调机制（这句话非常重要，如果没有理解这句话，也就无法理解 RedLock。并且由这句话我们可以得出，RedLock 依赖的环境不能是一个由 N 主 N 从组成的 Cluster 集群模式，因为 Cluster 模式下的各个 Master 并不完全独立，而是存在 Gossip 协调机制的）。
接下来，我们假设有 3 个完全相互独立的 Redis Master 单机节点，所以我们需要在 3 台机器上面运行这些实例，如下图所示（请注意这张图中 3 个 Master 节点完全相互独立）：
![image.png](https://a.perfma.net/img/2410485)

为了取到锁，客户端应该执行以下操作:

1. 获取当前 Unix 时间，以毫秒为单位。
2. 依次尝试从 N 个 Master 实例使用相同的 key 和随机值获取锁（假设这个 key 是 LOCK_KEY）。当向 Redis 设置锁时，客户端应该设置一个网络连接和响应超时时间，这个超时时间应该小于锁的失效时间。例如你的锁自动失效时间为 10 秒，则超时时间应该在 5-50 毫秒之间。这样可以避免服务器端 Redis 已经挂掉的情况下，客户端还在死死地等待响应结果。如果服务器端没有在规定时间内响应，客户端应该尽快尝试另外一个 Redis 实例。
3. 客户端使用当前时间减去开始获取锁时间（步骤 1 记录的时间）就得到获取锁使用的时间。当且仅当从大多数的 Redis 节点都取到锁，并且使用的时间小于锁失效时间时，锁才算获取成功。
4. 如果取到了锁，key 的真正有效时间等于有效时间减去获取锁所使用的时间（步骤 3 计算的结果）。
5. 如果因为某些原因，获取锁失败（没有在至少 N/2+1 个 Redis 实例取到锁或者取锁时间已经超过了有效时间），客户端应该在所有的 Redis 实例上进行解锁（即便某些 Redis 实例根本就没有加锁成功）。

## 基于 Redisson 实现 RedLock

RedLock 方案并不是很复杂，但是如果我们自己去实现一个工业级的 RedLock 方案还是有很多坑的。幸运的是，Redisson 已经为我们封装好了 RedLock 的开源实现，假设基于 3 个单机 Redis 实例实现 RedLock 分布式锁，即第二张图所示的 RedLock 方案，其源码如下所示，非常简单：

```java
Config config1 = new Config();
config1.useSingleServer().setAddress("redis://192.168.0.1:6379")
        .setPassword("afeiblog").setDatabase(0);
RedissonClient redissonClient1 = Redisson.create(config1);

Config config2 = new Config();
config2.useSingleServer().setAddress("redis://192.168.0.2:6379")
        .setPassword("afeiblog").setDatabase(0);
RedissonClient redissonClient2 = Redisson.create(config2);

Config config3 = new Config();
config3.useSingleServer().setAddress("redis://192.168.0.3:6379")
        .setPassword("afeiblog").setDatabase(0);
RedissonClient redissonClient3 = Redisson.create(config3);

String resourceName = "LOCK_KEY";

RLock lock1 = redissonClient1.getLock(resourceName);
RLock lock2 = redissonClient2.getLock(resourceName);
RLock lock3 = redissonClient3.getLock(resourceName);
// 向3个redis实例尝试加锁
RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3);
boolean isLock;
try {
    // isLock = redLock.tryLock();
    // 500ms拿不到锁, 就认为获取锁失败。10000ms即10s是锁失效时间。
    isLock = redLock.tryLock(500, 10000, TimeUnit.MILLISECONDS);
    System.out.println("isLock = "+isLock);
    if (isLock) {
        //TODO if get lock success, do something;
    }
} catch (Exception e) {
} finally {
    // 无论如何, 最后都要解锁
    redLock.unlock();
}
```

这段源码有几个要点：

- 首先需要构造 N 个 RLock（源码中是 3 个，RLock 就是普通的分布式锁）。
- 然后用这 N 个 RLock 构造一个 RedissonRedLock，这就是 Redisson 给我们封装好的 RedLock 分布式锁（即 N 个相互完全独立的节点）。
- 调用 unlock 方法解锁，这个方法会向每一个 RLock 发起解锁请求（for (RLock lock : locks) {futures.add(lock.unlockAsync());}）。

这段源码我们是基于 3 个完全独立的 Redis 单机实例来实现的（config1.useSingleServer()）。当然，我们也可以基于 3 个完全独立的主从（config.useMasterSlaveServers()），或者 3 个完全独立的 sentinel 集群（config.useSentinelServers()），或者 3 个完全独立的 Cluster 集群（config.useClusterServers().）。
假如我们依赖 3 个完全独立的 Cluster 集群来实现 ReLock 方案，那么架构图如下所示：

![image.png](https://a.perfma.net/img/2410511)

有些同学会反问，为什么需要 3 个 Redis Cluster，一个行不行？回答这个问题之前，我们假设只有一个 Redis Cluster，那么无论这个 Cluster 有多少个 Master，我们是没办法让 LOCK_KEY 发送到多个 Master 上的，因为一个 KEY 只会属于 Cluster 中的一个 Master，这一点也是没理解 RedLock 方案最容易犯的错误。
最后还有一个小小的注意点，Redis 分布式锁加锁时 value 必须唯一，RedLock 是怎么保证的呢？答案是 UUID + threadId，源码如下：

```
protected final UUID id = UUID.randomUUID();
String getLockName(long threadId) {
    return id + ":" + threadId;
}
```

## 写在最后

RedLock 方案相比普通的 Redis 分布式锁方案可靠性确实大大提升。但是，任何事情都具有两面性，因为我们的业务一般只需要一个 Redis Cluster，或者一个 Sentinel，但是这两者都不能承载 RedLock 的落地。如果你想要使用 RedLock 方案，还需要专门搭建一套环境。所以，如果不是对分布式锁可靠性有极高的要求（比如金融场景），不太建议使用 RedLock 方案。当然，作为基于 Redis 最牛的分布式锁方案，你依然必须掌握的非常好，以便在有需要时（比如面试）能应付自如。
