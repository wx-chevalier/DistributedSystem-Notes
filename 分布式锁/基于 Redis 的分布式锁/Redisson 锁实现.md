# Redisson 锁实现的解析

Redisson 是 Redis 的 Java 客户端，它提供了各种的 Redis 分布式锁的实现，如可重入锁、公平锁、RedLock、读写锁等等，并且在实现上考虑得也更加全面，适用于生产环境下使用。

### 4.1 分布式锁

使用 Redisson 来创建单机版本分布式锁非常简单，示例如下：

```java
// 1.创建RedissonClient,如果与spring集成，可以将RedissonClient声明为Bean,在使用时注入即可
Config config = new Config();
config.useSingleServer().setAddress("redis://192.168.0.100:6379");
RedissonClient redissonClient = Redisson.create(config);

// 2.创建锁实例
RLock lock = redissonClient.getLock("myLock");
try {
    //3.尝试获取分布式锁，第一个参数为等待时间，第二个参数为锁过期时间
    boolean isLock = lock.tryLock(10, 30, TimeUnit.SECONDS);
    if (isLock) {
        // 4.模拟业务处理
        System.out.println("处理业务逻辑");
        Thread.sleep(20 * 1000);
    }
} catch (Exception e) {
    e.printStackTrace();
} finally {
    //5.释放锁
    lock.unlock();
}
redissonClient.shutdown();
```

此时对应在 Redis 中的数据结构如下：

<div align="center"> <img src="https://gitee.com/heibaiying/Full-Stack-Notes/raw/master/pictures/redis_分布式锁_cli1.png"/> </div>

可以看到 key 就是代码中设置的锁名，而 value 值的类型是 hash，其中键 `9280e909-c86b-43ec-b11d-6e5a7745e2e9:13` 的格式为 `UUID + 线程ID` ；键对应的值为 1，代表加锁的次数。之所以要采用 hash 这种格式，主要是因为 Redisson 创建的锁是具有重入性的，即你可以多次进行加锁：

```java
boolean isLock1 = lock.tryLock(0, 30, TimeUnit.SECONDS);
boolean isLock2 = lock.tryLock(0, 30, TimeUnit.SECONDS);
```

此时对应的值就会变成 2，代表加了两次锁：

<div align="center"> <img src="https://gitee.com/heibaiying/Full-Stack-Notes/raw/master/pictures/redis_分布式锁_cli2.png"/> </div>

当然和其他重入锁一样，需要保证解锁的次数和加锁的次数一样，才能完全解锁：

```java
lock.unlock();
lock.unlock();
```

### 4.2 RedLock

Redisson 也实现了 Redis 官方推荐的 RedLock 方案，这里我们启动三个 Redis 实例进行演示，它们彼此之间可以是完全独立的，并不需要进行集群的相关配置：

```shell
$ ./redis-server ../redis.conf
$ ./redis-server ../redis.conf --port 6380
$ ./redis-server ../redis.conf --port 6381
```

对应的代码示例如下：

```java
// 1.创建RedissonClient
Config config01 = new Config();
config01.useSingleServer().setAddress("redis://192.168.0.100:6379");
RedissonClient redissonClient01 = Redisson.create(config01);
Config config02 = new Config();
config02.useSingleServer().setAddress("redis://192.168.0.100:6380");
RedissonClient redissonClient02 = Redisson.create(config02);
Config config03 = new Config();
config03.useSingleServer().setAddress("redis://192.168.0.100:6381");
RedissonClient redissonClient03 = Redisson.create(config03);

// 2.创建锁实例
String lockName = "myLock";
RLock lock01 = redissonClient01.getLock(lockName);
RLock lock02 = redissonClient02.getLock(lockName);
RLock lock03 = redissonClient03.getLock(lockName);

// 3. 创建 RedissonRedLock
RedissonRedLock redLock = new RedissonRedLock(lock01, lock02, lock03);

try {
    boolean isLock = redLock.tryLock(10, 300, TimeUnit.SECONDS);
    if (isLock) {
        // 4.模拟业务处理
        System.out.println("处理业务逻辑");
        Thread.sleep(200 * 1000);
    }
} catch (Exception e) {
    e.printStackTrace();
} finally {
    //5.释放锁
    redLock.unlock();
}

redissonClient01.shutdown();
redissonClient02.shutdown();
redissonClient03.shutdown();
```

此时每个 Redis 实例上锁的情况如下：

<div align="center"> <img src="https://gitee.com/heibaiying/Full-Stack-Notes/raw/master/pictures/redis_分布式锁_cli3.png"/> </div>

可以看到每个实例上都获得了锁。

### 4.3 延长锁时效

最后，介绍一下 Redisson 的 WatchDog 机制，它可以用来延长锁时效，示例如下：

```java
Config config = new Config();
// 1.设置WatchdogTimeout
config.setLockWatchdogTimeout(30 * 1000);
config.useSingleServer().setAddress("redis://192.168.0.100:6379");
RedissonClient redissonClient = Redisson.create(config);

// 2.创建锁实例
RLock lock = redissonClient.getLock("myLock");
try {
    //3.尝试获取分布式锁，第一个参数为等待时间
    boolean isLock = lock.tryLock(0, TimeUnit.SECONDS);
    if (isLock) {
        // 4.模拟业务处理
        System.out.println("处理业务逻辑");
        Thread.sleep(60 * 1000);
        System.out.println("锁剩余的生存时间：" + lock.remainTimeToLive());
    }
} catch (Exception e) {
    e.printStackTrace();
} finally {
    //5.释放锁
    lock.unlock();
}
redissonClient.shutdown();
```

首先 Redisson 的 WatchDog 机制只会对那些没有设置锁超时时间的锁生效，所以我们这里调用的是两个参数的 `tryLock()` 方法：

```java
boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
```

而不是包含超时时间的三个参数的 `tryLock()` 方法：

```java
boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;
```

其次我们通过 `config.setLockWatchdogTimeout(30 * 1000)` 将 lockWatchdogTimeout 的值设置为 30000 毫秒（默认值也是 30000 毫秒）。此时 Redisson 的 WatchDog 机制会以 lockWatchdogTimeout 的 1/3 时长为周期（在这里就是 10 秒）对所有未设置超时时间的锁进行检查，如果业务尚未处理完成（也就是锁还没有被程序主动删除），Redisson 就会将锁的超时时间重置为 lockWatchdogTimeout 指定的值（在这里就是设置的 30 秒），直到锁被程序主动删除位置。因此在上面的例子中可以看到，不论将模拟业务的睡眠时间设置为多长，其锁都会存在一定的剩余生存时间，直至业务处理完成。

反之，如果明确的指定了锁的超时时间 leaseTime，则以 leaseTime 的时间为准，因为 WatchDog 机制对明确指定超时时间的锁不会生效。

# Links

- https://mp.weixin.qq.com/s/9wDXAXVd9aEc_PaGqJMWIA 年轻人，看看 Redisson 分布式锁—可重入锁吧！太重要了
