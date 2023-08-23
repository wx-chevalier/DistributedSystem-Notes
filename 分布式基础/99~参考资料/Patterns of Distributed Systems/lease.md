# 租约（Lease）

**原文**

https://martinfowler.com/articles/patterns-of-distributed-systems/time-bound-lease.html

使用有时间限制的租约协调集群节点的活动。

**2021.1.13**

## 问题

集群节点需要对特定的资源进行排他性访问。但是，节点可能会崩溃；他们可能会临时失联，或是经历进程暂停。在这些出错的场景下，它们不应该无限地保持对资源的访问。

## 解决方案

集群节点可以申请一个有时间限制的租约，超过时间就过期。如果节点要延长访问时间，可以在到期前续租。用[一致性内核（Consistent Core）](consistent-core.md)实现租约机制，可以提供容错性和一致性。租约有一个“存活时间”值。租约可以在[领导者和追随者（Leader and Followers）](leader-and-followers.md)之间复制，以提供容错性。拥有租约的节点负责定期刷新它。[心跳（HeartBeat）](heartbeat.md)就是客户端用来更新在一致性内核中的存活时间值的。[一致性内核（Consistent Core）](consistent-core.md)中的所有节点都可以创建租约，但只有领导者要追踪租约的超时时间。一致性内核的追随者不用追踪超时时间。这么做是因为领导者要用自己的单调时钟决定租约何时过期，然后，让追随者知道租约何时过期。像其它决定一样，这样做可以保证，在[一致性内核（Consistent Core）](consistent-core.md)中，节点会对租约过期这件事能够达成共识。

当一个节点成为了领导者，它就开始追踪租约了。

```java
class ReplicatedKVStore…
  public void onBecomingLeader() {
      leaseTracker = new LeaderLeaseTracker(this, new SystemClock(), server);
      leaseTracker.start();
  }
```

领导者会启动一个调度任务，定期检查租约的过期情况。

```java
class LeaderLeaseTracker…
  private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
  private ScheduledFuture<?> scheduledTask;
  @Override
  public void start() {
      scheduledTask = executor.scheduleWithFixedDelay(this::checkAndExpireLeases,
              leaseCheckingInterval,
              leaseCheckingInterval,
              TimeUnit.MILLISECONDS);
  }

  @Override
  public void checkAndExpireLeases() {
      remove(expiredLeases());
  }

  private void remove(Stream<String> expiredLeases) {
      expiredLeases.forEach((leaseId)->{
          //remove it from this server so that it doesnt cause trigger again.
          expireLease(leaseId);
          //submit a request so that followers know about expired leases
          submitExpireLeaseRequest(leaseId);
      });
  }

  private Stream<String> expiredLeases() {
      long now = System.nanoTime();
      Map<String, Lease> leases = kvStore.getLeases();
      return  leases.keySet().stream()
              .filter(leaseId -> {
          Lease lease = leases.get(leaseId);
          return lease.getExpiresAt() < now;
      });
  }
```

追随者也会启动一个租约追踪器，但它没有任何的行为。

```java
class ReplicatedKVStore…
  public void onCandidateOrFollower() {
      if (leaseTracker != null) {
          leaseTracker.stop();
      }
      leaseTracker = new FollowerLeaseTracker(this, leases);
  }
```

租约可以简单地表示下面这样：

```java
public class Lease implements Logging {
    String name;
    long ttl;
    //Time at which this lease expires
    long expiresAt;

    //The keys from kv store attached with this lease
    List<String> attachedKeys = new ArrayList<>();

    public Lease(String name, long ttl, long now) {
        this.name = name;
        this.ttl = ttl;
        this.expiresAt = now + ttl;
    }

    public String getName() {
        return name;
    }

    public long getTtl() {
        return ttl;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void refresh(long now) {
        expiresAt = now + ttl;
        getLogger().info("Refreshing lease " + name + " Expiration time is " + expiresAt);
    }

    public void attachKey(String key) {
        attachedKeys.add(key);
    }

    public List<String> getAttachedKeys() {
        return attachedKeys;
    }
}
```

一个节点想要创建一个租约，它会先连接到[一致性内核（Consistent Core）](consistent-core.md)的领导者，然后，发送一个创建租约的请求。注册租约的请求会得到复制，其处理方式类似于[一致性内核（Consistent Core）](consistent-core.md)中的其它请求。只有在[高水位标记（High-Water Mark）](high-water-mark.md)到达这个请求条目在复制日志中的日志索引之后，请求才算处理完成。

```java
class ReplicatedKVStore…
  private ConcurrentHashMap<String, Lease> leases = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Response> registerLease(String name, long ttl) {
      if (leaseExists(name)) {
          return CompletableFuture
                .completedFuture(
                        Response.error(Errors.DUPLICATE_LEASE_ERROR,
                            "Lease with name " + name + " already exists"));
      }
      return server.propose(new RegisterLeaseCommand(name, ttl));
  }

  private boolean leaseExists(String name) {
      return leases.containsKey(name);
  }
```

有一点需要注意，在哪里验证租约注册是否重复。在提出请求之前检查是不够的，因为可能会存在多个在途请求。因此，服务器要在成功复制之后，它还要检查租约注册是否重复。

```java
class LeaderLeaseTracker…
  private Map<String, Lease> leases;
  @Override
  public void addLease(String name, long ttl) throws DuplicateLeaseException {
      if (leases.get(name) != null) {
          throw new DuplicateLeaseException(name);
      }
      Lease lease = new Lease(name, ttl, clock.nanoTime());
      leases.put(name, lease);
  }
```

![注册租约](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/register-lease.png)

<center>图1：注册租约</center>

负责租约的节点会连接到领导者上，在租约过期之前刷新租约。正如在[心跳（HeartBeat）](heartbeat.md)中所讨论的，它需要考虑网络的往返时间以决定“存活时间”的值。在“存活时间”间隔内，节点可以多次发出刷新请求，以确保租约在任何问题下都能刷新。但是，节点也要保证不会发送太多的刷新请求。一种合理的做法是，租约时间过半时发送请求。这样一来，在租约时间内，最多发送两次刷新请求。客户端节点可以用自己的单调时钟来跟踪时间。

```java
class LeaderLeaseTracker…
  @Override
  public void refreshLease(String name) {
      Lease lease = leases.get(name);
      lease.refresh(clock.nanoTime());
  }
```

刷新请求只会发送给一致性内核的领导者，因为只有领导者负责决策租约何时过期。

![刷新租约](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/refresh-lease.png)

<center>图2：刷新租约</center>

租约过期后，领导者就会删除它。将这个信息提交到[一致性内核（Consistent Core）](consistent-core.md)，这也是至关重要的。因此，领导者会发送一个请求，让租约过期，就像[一致性内核（Consistent Core）](consistent-core.md)处理其它请求一样。一旦[高水位标记（High-Water Mark）](high-water-mark.md)到达了提议的租约过期请求。它就从所有的追随者中彻底删除了。

```java
class LeaderLeaseTracker…
  public void expireLease(String name) {
      getLogger().info("Expiring lease " + name);
      Lease removedLease = leases.remove(name);
      removeAttachedKeys(removedLease);
  }
```

![过期租约](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/expire-lease.png)

<center>图3：过期租约</center>

### 在键值存储中将租约与键值关联起来

集群需要知道其节点是否失效。可以这样做，节点从一致性内核中获取一个租约，然后，将它和用来识别自身的键值关联起来，存储在一致性内核里。如果集群节点在运行，它应该定期延续租约。如果租约过期，关联的键值就会删除掉。键值删除之后，就会给所有对此感兴趣的集群节点发出一个事件，表示节点失效，这在[状态监控（State Watch）](state-watch.md)模式中已经讨论过了。

使用一致性内核，集群节点可以用一次网络调用创建一个租约，就像下面这样：

```java
consistentCoreClient.registerLease("server1Lease", TimeUnit.SECONDS.toNanos(5));
```

然后，将租约和一致性内核上存储的识别自身的键值关联起来。

```java
consistentCoreClient.setValue("/servers/1", "{address:192.168.199.10, port:8000}", "server1Lease");
```

一致性内核接收到消息后，把键值保存在键值存储中，它还会将键值同这个特定的租约关联在一起。

```java
class ReplicatedKVStore…
  private ConcurrentHashMap<String, Lease> leases = new ConcurrentHashMap<>();

class ReplicatedKVStore…
  private Response applySetValueCommand(Long walEntryId, SetValueCommand setValueCommand) {
      getLogger().info("Setting key value " + setValueCommand);
      if (setValueCommand.hasLease()) {
          Lease lease = leases.get(setValueCommand.getAttachedLease());
          if (lease == null) {
              //The lease to attach is not available with the Consistent Core
              return Response.error(Errors.NO_LEASE_ERROR,
                      "No lease exists with name "
                              + setValueCommand.getAttachedLease());
          }
          lease.attachKey(setValueCommand.getKey());
      }
      kv.put(setValueCommand.getKey(), new StoredValue(setValueCommand.getValue(), walEntryId));
```

一旦租约过期，一致性内核也会从键值存储中删除关联的键值。

```java
class LeaderLeaseTracker…
  public void expireLease(String name) {
      getLogger().info("Expiring lease " + name);
      Lease removedLease = leases.remove(name);
      removeAttachedKeys(removedLease);
  }

  private void removeAttachedKeys(Lease removedLease) {
      if (removedLease == null) {
          return;
      }
      List<String> attachedKeys = removedLease.getAttachedKeys();
      for (String attachedKey : attachedKeys) {
          getLogger().trace("Removing " + attachedKey + " with lease " + removedLease);
          kvStore.remove(attachedKey);
      }
  }
```

### 处理领导者失效

当既有的领导者失效了，[一致性内核（Consistent Core）](consistent-core.md)会选出一个新的领导者。一旦当选，新的领导者就要开始追踪租约。

新的领导者会刷新它所知道的所有租约。请注意，原有领导者上即将过期的租约会延长一个“存活时间”的值。这不是大问题，因为它给了客户端一个机会，重连到新的领导者，延续租约。

```java
private void refreshLeases() {
    long now = clock.nanoTime();
    this.kvStore.getLeases().values().forEach(l -> {
        l.refresh(now);
    });
}
```

![在新的领导者上追踪租约](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/lease-transfer-on-new-leader.png)

<center>图4：在新的领导者上追踪租约</center>

![在新的领导上刷新租约](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/refresh-on-new-leader.png)

<center>图5：在新的领导上刷新租约</center>

## 示例

- Google 的 [chubby](https://research.google/pubs/pub27897/) 服务实现了类似的基于时间限制的租约机制。
- [zookeeper](https://zookeeper.apache.org/) 的会话管理采用了类似于复制租约的机制。
- Kafka 的 [kip-631](https://cwiki.apache.org/confluence/display/KAFKA/KIP-631%3A+The+Quorum-based+Kafka+Controller) 提出使用有时间限制的租约，对分组成员信息进行管理。
- [etcd](https://etcd.io/) 提供了有时间限制的租约设施，客户端可以用其协调其活动，以及分组成员信息和失效检测。
- [dhcp](https://en.wikipedia.org/wiki/Dynamic_Host_Configuration_Protocol) 协议允许连接的设备租用一个 IP 地址。多台 DHCP 服务器的[故障恢复协议](https://tools.ietf.org/html/draft-ietf-dhc-failover-12)，其工作原理类似于这里阐述的实现。
