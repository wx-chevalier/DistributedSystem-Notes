# 两阶段提交（Two Phase Commit）

**原文**

https://martinfowler.com/articles/patterns-of-distributed-systems/two-phase-commit.html

在一个原子操作里更新多个节点的资源。

**2021.1.18**

## 问题

当数据需要在多个集群节点上进行原子化地存储时，集群节点在了解其他集群节点的决定之前，是不能让客户端进行访问数据的。每个节点都需要知道其它的节点是成功地存储数据，还是失败了。

## 解决方案

不出所料，两阶段提交的本质是，将一次更新分为两个阶段执行：

- 第一阶段，预备，询问每个节点是否能够承诺执行更新。
- 第二阶段，提交，实际地执行更新。

在预备阶段，参与事务的每个节点都要获得其所需的内容，以确保能够在第二阶段完成提交，例如，必需的锁。一旦每个节点都能确保在第二阶段完成提交，它就可以让协调者知道，对协调者做出有效地承诺，然后，在第二阶段进行提交。如果其中任何一个节点无法做出承诺，协调者将会通知所有节点回滚，释放其持有的锁，事务也将终止。只有在所有的参与者都同意进入下一阶段，第二阶段才会开始，届时，它们都将成功地进行更新。

考虑一个简单的分布式键值存储，两阶段提交协议的工作原理如下。

事务型的客户端会创建一个唯一标识符，称为事务标识符。客户端还会记下其他的一些细节，比如事务的起始时间。这是用来防止死锁的，稍后讲到锁机制再来详述。客户机记下的唯一 id 以及其他细节（比如起始时间戳）用于跨集群节点引用事务。客户端维护一个事务引用，它会随着客户端的每个请求传递给其它的集群节点，就像下面这样：

```java
class TransactionRef…

  private UUID txnId;
  private long startTimestamp;


  public TransactionRef(long startTimestamp) {
      this.txnId = UUID.randomUUID();
      this.startTimestamp = startTimestamp;
  }

class TransactionClient…

  TransactionRef transactionRef;

  public TransactionClient(ReplicaMapper replicaMapper, SystemClock systemClock) {
      this.clock = systemClock;
      this.transactionRef = new TransactionRef(clock.now());
      this.replicaMapper = replicaMapper;
  }
```

集群中会有一个节点扮演协调者的角色，用以代表客户端去跟踪事务的状态。在一个键值存储中，它通常是存储某个键值对应数据的集群节点。一般而言，选中的是客户端使用的第一个键值对应存储数据的集群节点。

在存储任何值之前，客户端都会与协调者进行通信，通知它即将开启一个事务。因为协调者同时也是存储值的某个节点，因此，当客户端对特定键值发起 get 或 put 操作时，也会动态地选到它。

```java
class TransactionClient…

  private TransactionalKVStore coordinator;
  private void maybeBeginTransaction(String key) {
      if (coordinator == null) {
          coordinator = replicaMapper.serverFor(key);
          coordinator.begin(transactionRef);
      }
  }
```

事务协调者会跟踪事务状态。它会将每个变化都记录到[预写日志（Write-Ahead Log）](write-ahead-log.md)中，以确保这些细节在发生故障时可用。

```java
class TransactionCoordinator…

  Map<TransactionRef, TransactionMetadata> transactions = new ConcurrentHashMap<>();
  WriteAheadLog transactionLog;

  public void begin(TransactionRef transactionRef) {
      TransactionMetadata txnMetadata = new TransactionMetadata(transactionRef, systemClock, transactionTimeoutMs);
      transactionLog.writeEntry(txnMetadata.serialize());
      transactions.put(transactionRef, txnMetadata);
  }
class TransactionMetadata…

  private TransactionRef txn;
  private List<String> participatingKeys = new ArrayList<>();
  private TransactionStatus transactionStatus;
```

客户端会将每个键值都当做事务的一部分发送给协调者。这样，协调者就可以跟踪属于该事务的所有键值。在事务的元数据中，协调者也会记录属于该事务的所有键值。这些键值也可以用来了解所有参与事务的集群节点。因为每一个键值对一般都是通过[复制日志（Replicated Log）](replicated-log.md)进行复制，处理某个特定键值请求的领导者服务器可能会在事务的声明周期中一直在改变，因此，跟踪的是键值，而非实际的服务器地址。客户端发送的 put 或 get 请求就会抵达持有这个键值数据的服务器。服务器的选择是基于分区策略的。值得注意的是，客户端同服务器之间是直接通信，无需经过协调者。这就避免了在网络上发送两次数据：从客户端到协调者，再由协调者到相应的服务器。

```java
class TransactionClient…

  public CompletableFuture<String> get(String key) {
      maybeBeginTransaction(key);
      coordinator.addKeyToTransaction(transactionRef, key);
      TransactionalKVStore kvStore = replicaMapper.serverFor(key);
      return kvStore.get(transactionRef, key);
  }

  public void put(String key, String value) {
      maybeBeginTransaction(key);
      coordinator.addKeyToTransaction(transactionRef, key);
      replicaMapper.serverFor(key).put(transactionRef, key, value);
  }
class TransactionCoordinator…

  public synchronized void addKeyToTransaction(TransactionRef transactionRef, String key) {
      TransactionMetadata metadata = transactions.get(transactionRef);
      if (!metadata.getParticipatingKeys().contains(key)) {
          metadata.addKey(key);
          transactionLog.writeEntry(metadata.serialize());
      }
  }
```

通过事务 ID，处理请求的集群节点可以检测到请求是事务的一部分。它会对事务的状态进行管理，其中存放有请求中的键值对。键值对并不会直接提供给键值存储，而是进行了单独地存储。

```java
class TransactionalKVStore…

  public void put(TransactionRef transactionRef, String key, String value) {
      TransactionState state = getOrCreateTransactionState(transactionRef);
      state.addPendingUpdates(key, value);
  }
```

### 锁与事务隔离

请求还会给键值加锁。尤其是，get 请求会加上读锁，而 put 请求则会加写锁。读取值的时候，要去获取读锁。

```java
class TransactionalKVStore…

  public CompletableFuture<String> get(TransactionRef txn, String key) {
      CompletableFuture<TransactionRef> lockFuture
              = lockManager.acquire(txn, key, LockMode.READ);
      return lockFuture.thenApply(transactionRef -> {
          getOrCreateTransactionState(transactionRef);
          return kv.get(key);
      });
  }

  synchronized TransactionState getOrCreateTransactionState(TransactionRef txnRef) {
      TransactionState state = this.ongoingTransactions.get(txnRef);
      if (state == null) {
          state = new TransactionState();
          this.ongoingTransactions.put(txnRef, state);
      }
      return state;
  }
```

当事务即将提交，值会在键值存储中变得可见时，才会获取写锁。在此之前，集群节点只能将修改的值当做一个待处理的操作。

延迟加锁会降低事务冲突的几率。

```java
class TransactionalKVStore…

  public void put(TransactionRef transactionRef, String key, String value) {
      TransactionState state = getOrCreateTransactionState(transactionRef);
      state.addPendingUpdates(key, value);
  }
```

值得注意的是，这些锁是长期存在的，请求完成之后并不释放。只有事务提交时，才会释放这些锁。这种在事务期间持有锁，仅在事务提交或回滚时释放的技术称为 [两阶段锁定(2PL two-phase-locking)](https://en.wikipedia.org/wiki/Two-phase_locking)。对于提供串行隔离级别（serializable isolation level）而言，两阶段锁定至关重要。串行意味着，事务的效果就像一次一个地执行。

#### 防止死锁

如果采用锁，两个事务等待彼此释放锁的场景就可能会引起死锁。检测到冲突时，如果不允许事务等待，立即终止，这样就可以避免死锁。有不同的策略可以决定哪些事务要立即终止，哪些允许继续。

锁管理器（Lock Manager）可以像下面这样实现等待策略（Wait Policy）：

```java
class LockManager…

  WaitPolicy waitPolicy;
```

`WaitPolicy` 决定在请求发生冲突时要做什么。

```java
public enum WaitPolicy {
    WoundWait,
    WaitDie,
    Error
}
```

锁是一个对象，它会记录当前拥有锁的事务，以及和等待该锁的事务。

```
class Lock…

  Queue<LockRequest> waitQueue = new LinkedList<>();
  List<TransactionRef> owners = new ArrayList<>();
  LockMode lockMode;
```

当事务请求获取锁时，如果没有冲突的事务已经拥有了该锁，那么锁管理器就会立即将该锁授予它。

```java
class LockManager…

  public synchronized CompletableFuture<TransactionRef> acquire(TransactionRef txn, String key, LockMode lockMode) {
      return acquire(txn, key, lockMode, new CompletableFuture<>());
  }

  CompletableFuture<TransactionRef> acquire(TransactionRef txnRef,
                                            String key,
                                            LockMode askedLockMode,
                                            CompletableFuture<TransactionRef> lockFuture) {
      Lock lock = getOrCreateLock(key);

      logger.debug("acquiring lock for = " + txnRef + " on key = " + key + " with lock mode = " + askedLockMode);
      if (lock.isCompatible(txnRef, askedLockMode)) {
          lock.addOwner(txnRef, askedLockMode);
          lockFuture.complete(txnRef);
          logger.debug("acquired lock for = " + txnRef);
          return lockFuture;
      }
class Lock…

  public boolean isCompatible(TransactionRef txnRef, LockMode lockMode) {
      if(hasOwner()) {
          return (inReadMode() && lockMode == LockMode.READ)
                  || isUpgrade(txnRef, lockMode);
      }
      return true;
  }
```

如果发生冲突，锁管理器的行为就取决于等待策略了。

##### 冲突时抛出错误（Error On Conflict）

如果等待策略是抛出错误（error out），它就会抛出一个错误，调用的事务就会回滚，随机等待一段时间后进行重试。

```java
class LockManager…

  private CompletableFuture<TransactionRef> handleConflict(Lock lock,
                                                           TransactionRef txnRef,
                                                           String key,
                                                           LockMode askedLockMode,
                                                           CompletableFuture<TransactionRef> lockFuture) {
      switch (waitPolicy) {
          case Error: {
              lockFuture.completeExceptionally(new WriteConflictException(txnRef, key, lock.owners));
              return lockFuture;
          }
          case WoundWait: {
              return lock.woundWait(txnRef, key, askedLockMode, lockFuture, this);
          }
          case WaitDie: {
              return lock.waitDie(txnRef, key, askedLockMode, lockFuture, this);
          }
      }
      throw new IllegalArgumentException("Unknown waitPolicy " + waitPolicy);
  }
```

在许多用户事务尝试获取锁引发竞争的情况下，如果所有的事务都需要重新启动，就会严重限制系统的吞吐量。数据存储会尝试确保事务重启的次数最少。

一种常见的技术是，给事务分配一个唯一 ID，并给它们排序。比如，[Spanner](https://cloud.google.com/spanner)为事务[分配了唯一 ID](https://dahliamalkhi.github.io/files/SpannerExplained-SIGACT2013b.pdf)，这样就可以对它们排序了。这与 [Paxos](paxos.md) 中讨论的跨集群节点排序请求技术非常相似，有两种技术用于避免死锁，但依然要允许事务能够在不重启的情况下继续。

事务引用（transaction reference）的创建方式应该是可以与其它事务的引用进行比较和排序。最简单的方法是给每个事务分配一个时间戳，并根据时间戳进行比较。

```java
class TransactionRef…

  boolean after(TransactionRef otherTransactionRef) {
      return this.startTimestamp > otherTransactionRef.startTimestamp;
  }
```

但是在分布式系统中，[挂钟时间不是单调的](https://martinfowler.com/articles/patterns-of-distributed-systems/time-bound-lease.html#wall-clock-not-monotonic)，因此，会采用不同的方式给事务分配唯一 ID，保证事务可以排序。除了这些可排序的 ID，还要追踪每个事务的年龄，这样就能对事务排序了。[Spanner](https://cloud.google.com/spanner) 就是通过追踪系统中每个事务的年龄为事务排序。

为了能够对所有的事务排序，每个集群节点都要分配一个唯一 ID。事务开始时，客户端会选择一个协调者，从协调者获取到事务 ID。扮演协调者的集群节点会生成事务 ID，就像下面这样。

```java
class TransactionCoordinator…

  private int requestId;
  public MonotonicId begin() {
      return new MonotonicId(requestId++, config.getServerId());
  }
class MonotonicId…

  public class MonotonicId implements Comparable<MonotonicId> {
      public int requestId;
      int serverId;

      public MonotonicId(int requestId, int serverId) {
          this.serverId = serverId;
          this.requestId = requestId;
      }

      public static MonotonicId empty() {
          return new MonotonicId(-1, -1);
      }

      public boolean isAfter(MonotonicId other) {
          if (this.requestId == other.requestId) {
              return this.serverId > other.serverId;
          }
          return this.requestId > other.requestId;
      }
class TransactionClient…

  private void beginTransaction(String key) {
      if (coordinator == null) {
          coordinator = replicaMapper.serverFor(key);
          MonotonicId transactionId = coordinator.begin();
          transactionRef = new TransactionRef(transactionId, clock.nanoTime());
      }
  }
```

客户端会记录事务开始至今流逝的时间，以此作为事务的年龄。

```java
class TransactionRef…

  public void incrementAge(SystemClock clock) {
      age = clock.nanoTime() - startTimestamp;
  }
```

每当客户端向服务器发起 get 或 put 请求时，都会递增事务的年龄。然后，事务会根据其年龄进行排序。对于同样年龄的事务，则会比较事务 ID。

```java
class TransactionRef…

  public boolean isAfter(TransactionRef other) {
       return age == other.age?
                  this.id.isAfter(other.id)
                  :this.age > other.age;
  }
```

### Wound-Wait

采用 [Wound-Wait](http://www.mathcs.emory.edu/~cheung/Courses/554/Syllabus/8-recv+serial/deadlock-woundwait.html) 策略，如果发生冲突，请求锁的事务引用将与当前拥有该锁的所有事务进行比较。如果锁的拥有者比请求锁的事务年轻，所有这些事务都会终止。但是，如果请求锁的事务比拥有锁的事务年轻，那它就要继续等待锁了。

```java
class Lock…

  public CompletableFuture<TransactionRef> woundWait(TransactionRef txnRef,
                                                     String key,
                                                     LockMode askedLockMode,
                                                     CompletableFuture<TransactionRef> lockFuture,
                                                     LockManager lockManager) {

      if (allOwningTransactionsStartedAfter(txnRef) && !anyOwnerIsPrepared(lockManager)) {
          abortAllOwners(lockManager, key, txnRef);
          return lockManager.acquire(txnRef, key, askedLockMode, lockFuture);
      }

      LockRequest lockRequest = new LockRequest(txnRef, key, askedLockMode, lockFuture);
      lockManager.logger.debug("Adding to wait queue = " + lockRequest);
      addToWaitQueue(lockRequest);
      return lockFuture;
  }
class Lock…

  private boolean allOwningTransactionsStartedAfter(TransactionRef txn) {
      return owners.stream().filter(o -> !o.equals(txn)).allMatch(owner -> owner.after(txn));
  }
```

值得注意的一个关键点是，如果拥有锁的事务已经处于两阶段提交的准备状态，它不会中止的。

### Wait-Die

[Wait-Die](http://www.mathcs.emory.edu/~cheung/Courses/554/Syllabus/8-recv+serial/deadlock-waitdie.html) 方法的工作方式与 [Wound-Wait](http://www.mathcs.emory.edu/~cheung/Courses/554/Syllabus/8-recv+serial/deadlock-woundwait.html) 截然相反。如果锁拥有者都比请求锁的事务都年轻，那该事务就要等待锁。但是，如果请求锁的事务比一部分拥有锁的事务年轻，那么该事务就要终止。

```java
class Lock…

  public CompletableFuture<TransactionRef> waitDie(TransactionRef txnRef,
                                                   String key,
                                                   LockMode askedLockMode,
                                                   CompletableFuture<TransactionRef> lockFuture,
                                                   LockManager lockManager) {
      if (allOwningTransactionsStartedAfter(txnRef)) {
          addToWaitQueue(new LockRequest(txnRef, key, askedLockMode, lockFuture));
          return lockFuture;
      }

      lockManager.abort(txnRef, key);
      lockFuture.completeExceptionally(new WriteConflictException(txnRef, key, owners));
      return lockFuture;
  }
```

相比于 Wait-Die 方法，Wound-Wait 机制通常[重启次数更少](http://www.mathcs.emory.edu/~cheung/Courses/554/Syllabus/8-recv+serial/deadlock-compare.html)。因此，像 [Spanner](https://cloud.google.com/spanner)这样的数据存储采纳 [Wound-Wait](http://www.mathcs.emory.edu/~cheung/Courses/554/Syllabus/8-recv+serial/deadlock-woundwait.html)

当事务的所有者释放锁时，等待中的事务会被授予锁。

```java
class LockManager…

  private void release(TransactionRef txn, String key) {
      Optional<Lock> lock = getLock(key);
      lock.ifPresent(l -> {
          l.release(txn, this);
      });
  }
class Lock…

  public void release(TransactionRef txn, LockManager lockManager) {
      removeOwner(txn);
      if (hasWaiters()) {
          LockRequest lockRequest = getFirst(lockManager.waitPolicy);
          lockManager.acquire(lockRequest.txn, lockRequest.key, lockRequest.lockMode, lockRequest.future);
      }
  }
```

## 提交与回滚

一旦客户端在没有遇到任何冲突的情况下成功读取并写入所有键值，它就会发一个提交请求给协调者，以此发起这次的提交请求。

```java
class TransactionClient…

  public CompletableFuture<Boolean> commit() {
      return coordinator.commit(transactionRef);
  }
```

事务协调者会将这次的事务状态记录为预备提交。协调者会分两个阶段实现这次提交。

- 首先，向每个参与者发送预备请求。
- 一旦协调者收到所有参与者的成功应答，协调者就会把事务标记为准备完成，然后，把提交请求发送给所有的参与者。

```java
class TransactionCoordinator…

  public CompletableFuture<Boolean> commit(TransactionRef transactionRef)  {
      TransactionMetadata metadata = transactions.get(transactionRef);
      metadata.markPreparingToCommit(transactionLog);
      List<CompletableFuture<Boolean>> allPrepared = sendPrepareRequestToParticipants(transactionRef);
      CompletableFuture<List<Boolean>> futureList = sequence(allPrepared);
      return futureList.thenApply(result -> {
          if (!result.stream().allMatch(r -> r)) {
              logger.info("Rolling back = " + transactionRef);
              rollback(transactionRef);
              return false;
          }
          metadata.markPrepared(transactionLog);
          sendCommitMessageToParticipants(transactionRef);
          metadata.markCommitComplete(transactionLog);
          return true;
      });
  }

  public List<CompletableFuture<Boolean>> sendPrepareRequestToParticipants(TransactionRef transactionRef)  {
      TransactionMetadata transactionMetadata = transactions.get(transactionRef);
      var transactionParticipants = getParticipants(transactionMetadata.getParticipatingKeys());
      return transactionParticipants.keySet()
              .stream()
              .map(server -> server.handlePrepare(transactionRef))
              .collect(Collectors.toList());
  }

  private void sendCommitMessageToParticipants(TransactionRef transactionRef) {
      TransactionMetadata transactionMetadata = transactions.get(transactionRef);
      var participantsForKeys = getParticipants(transactionMetadata.getParticipatingKeys());
      participantsForKeys.keySet().stream()
              .forEach(kvStore -> {
                  List<String> keys = participantsForKeys.get(kvStore);
                  kvStore.handleCommit(transactionRef, keys);
              });
  }

  private Map<TransactionalKVStore, List<String>> getParticipants(List<String> participatingKeys) {
      return participatingKeys.stream()
              .map(k -> Pair.of(serverFor(k), k))
              .collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
  }
```

收到准备请求的集群节点会做两件事：

- 试图抓到所有键值的写锁。
- 一旦成功，它会将所有变更写入[预写日志（Write-Ahead Log）](write-ahead-log.md)。

如果成功地做到这些，它就能保证没有冲突的事务，即使在崩溃的情况下，集群节点也能恢复完成事务所必需的状态。

```java
class TransactionalKVStore…

  public synchronized CompletableFuture<Boolean> handlePrepare(TransactionRef txn) {
      try {
          TransactionState state = getTransactionState(txn);
          if (state.isPrepared()) {
              return CompletableFuture.completedFuture(true); //already prepared.
          }

          if (state.isAborted()) {
              return CompletableFuture.completedFuture(false); //aborted by another transaction.
          }

          Optional<Map<String, String>> pendingUpdates = state.getPendingUpdates();
          CompletableFuture<Boolean> prepareFuture = prepareUpdates(txn, pendingUpdates);
          return prepareFuture.thenApply(ignored -> {
              Map<String, Lock> locksHeldByTxn = lockManager.getAllLocksFor(txn);
              state.markPrepared();
              writeToWAL(new TransactionMarker(txn, locksHeldByTxn, TransactionStatus.PREPARED));
              return true;
          });

      } catch (TransactionException| WriteConflictException e) {
          logger.error(e);
      }
      return CompletableFuture.completedFuture(false);
  }

  private CompletableFuture<Boolean> prepareUpdates(TransactionRef txn, Optional<Map<String, String>> pendingUpdates)  {
      if (pendingUpdates.isPresent()) {
          Map<String, String> pendingKVs = pendingUpdates.get();
          CompletableFuture<List<TransactionRef>> lockFuture = acquireLocks(txn, pendingKVs.keySet());
          return lockFuture.thenApply(ignored -> {
              writeToWAL(txn, pendingKVs);
              return true;
          });
      }
      return CompletableFuture.completedFuture(true);
  }

  TransactionState getTransactionState(TransactionRef txnRef) {
      return ongoingTransactions.get(txnRef);
  }

  private void writeToWAL(TransactionRef txn, Map<String, String> pendingUpdates) {
     for (String key : pendingUpdates.keySet()) {
          String value = pendingUpdates.get(key);
          wal.writeEntry(new SetValueCommand(txn, key, value).serialize());
      }
  }

  private CompletableFuture<List<TransactionRef>> acquireLocks(TransactionRef txn, Set<String> keys) {
      List<CompletableFuture<TransactionRef>> lockFutures = new ArrayList<>();
      for (String key : keys) {
          CompletableFuture<TransactionRef> lockFuture = lockManager.acquire(txn, key, LockMode.READWRITE);
          lockFutures.add(lockFuture);
      }
      return sequence(lockFutures);
  }
```

当集群节点收到来自协调器的提交消息时，让键值变化可见就是安全的。提交这个变化时，集群节点可以做三件事：

- 将事务标记为已提交。如果集群节点此时发生故障，因为它知道事务的结果，所以，可以重复下述步骤。
- 将所有变更应用到键值存储上。
- 释放所有已获得的锁。

```java
class TransactionalKVStore…

  public synchronized void handleCommit(TransactionRef transactionRef, List<String> keys) {
      if (!ongoingTransactions.containsKey(transactionRef)) {
          return; //this is a no-op. Already committed.
      }

      if (!lockManager.hasLocksFor(transactionRef, keys)) {
          throw new IllegalStateException("Transaction " + transactionRef + " should hold all the required locks for keys " + keys);
      }

      writeToWAL(new TransactionMarker(transactionRef, TransactionStatus.COMMITTED, keys));

      applyPendingUpdates(transactionRef);

      releaseLocks(transactionRef, keys);
  }

  private void removeTransactionState(TransactionRef txnRef) {
      ongoingTransactions.remove(txnRef);
  }


  private void applyPendingUpdates(TransactionRef txnRef) {
      TransactionState state = getTransactionState(txnRef);
      Optional<Map<String, String>> pendingUpdates = state.getPendingUpdates();
      apply(txnRef, pendingUpdates);
  }

  private void apply(TransactionRef txnRef, Optional<Map<String, String>> pendingUpdates) {
      if (pendingUpdates.isPresent()) {
          Map<String, String> pendingKv = pendingUpdates.get();
          apply(pendingKv);
      }
      removeTransactionState(txnRef);
  }

  private void apply(Map<String, String> pendingKv) {
      for (String key : pendingKv.keySet()) {
          String value = pendingKv.get(key);
          kv.put(key, value);
      }
  }
  private void releaseLocks(TransactionRef txn, List<String> keys) {
          lockManager.release(txn, keys);
  }

  private Long writeToWAL(TransactionMarker transactionMarker) {
     return wal.writeEntry(transactionMarker.serialize());
  }
```

回滚的实现方式是类似的。如果有任何失败，客户端都要与协调器通信回滚事务。

```java
class TransactionClient…

  public void rollback() {
      coordinator.rollback(transactionRef);
  }
```

事务协调者将事务的状态记录为预备回滚。然后，它将回滚请求转发给所有存储了给定事务的值对应的服务器。一旦所有请求都成功，协调器就将事务回滚标记为完成。如果在事务标记为“预备回滚”后，协调器崩溃了，它在恢复后依然可以继续向所有参与的集群节点发送回滚消息。

```java
class TransactionCoordinator…

  public void rollback(TransactionRef transactionRef) {
      transactions.get(transactionRef).markPrepareToRollback(this.transactionLog);

      sendRollbackMessageToParticipants(transactionRef);

      transactions.get(transactionRef).markRollbackComplete(this.transactionLog);
  }

  private void sendRollbackMessageToParticipants(TransactionRef transactionRef) {
      TransactionMetadata transactionMetadata = transactions.get(transactionRef);
      var participants = getParticipants(transactionMetadata.getParticipatingKeys());
      for (TransactionalKVStore kvStore : participants.keySet()) {
          List<String> keys = participants.get(kvStore);
          kvStore.handleRollback(transactionMetadata.getTxn(), keys);
      }
  }
```

回滚请求的参与者会做三件事：

- 在预写日志中记录事务的状态为回滚。
- 丢弃事务的状态。
- 释放所有的锁。

```java
class TransactionalKVStore…

  public synchronized void handleRollback(TransactionRef transactionRef, List<String> keys) {
      if (!ongoingTransactions.containsKey(transactionRef)) {
          return; //no-op. Already rolled back.
      }
      writeToWAL(new TransactionMarker(transactionRef, TransactionStatus.ROLLED_BACK, keys));
      this.ongoingTransactions.remove(transactionRef);
      this.lockManager.release(transactionRef, keys);
  }
```

### 幂等操作

在网络失效的情况下，协调者可能会对预备、提交或终止的调用进行重试。因此，这些操作需要是[幂等的](idempotent-receiver.md)。

### 示例场景

#### 原子写入

请考虑以下情况。Paula Blue 有一辆卡车，Steven Green 有一台挖掘机。卡车和挖掘机的可用性和预订状态都存储在一个分布式的键值存储中。根据键值映射到服务器的方式，Blue 的卡车和 Green 的挖掘机的预订存储在不同的集群节点上。Alice 正尝试为计划周一开始的建筑工作预订一辆卡车和挖掘机。她需要卡车和挖掘机二者能够同时可用。

预订场景的发生过程如下所示。

通过读取'truck_booking_monday'和'backhoe_booking_monday'这两个键值，Alice 可以检查 Blue 的卡车和 Green 的挖掘机的可用性。

![检查卡车](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/2pc/blue_get_truck_availability.png)

<center>检查卡车</center>

![检查挖土机](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/2pc/blue_get_backhoe_availability.png)

<center>检查挖土机</center>

如果两个值都为空，就可以预定了。她可以预定卡车和挖掘机。有一点很重要，两个值的设置是原子化的。任何一个失败了，二者都不会设置成功。

提交分两个阶段进行。Alice 联系的第一个服务器扮演协调者，执行这两个阶段。

![提交成功](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/2pc/blue_commit_success.png)

<center>提交成功</center>

_在这个协议中，协调者是一个独立的参与者，顺序图中就是如此显示的。然而，其中的一台服务器（Blue 或 Green）会扮演协调者的角色，因此，它会在交互中承担两个角色。_

#### 事务冲突

考虑这样一个场景，另有一个人 Bob，他 也试图在同一个周一为建筑工作预订一辆卡车和挖掘机。

预订场景按照下面的情形进行：

- Alice 和 Bob 都去读键值'truck_booking_monday'和'backhoe_booking_monday'
- 二者都看到值为空，意味着预定。
- 二者都试图预定卡车和挖掘机。

预期是这样的，Alice 或 Bob 只有一个人能够预定，因为其事务是冲突的。在出现错误的情况下，整个流程需要重试，但愿有一个人能够继续预定。但任何情况下，预定都不应该是部分完成。两个预定要么都完成，要么都不完成。

为了检查可用性，Alice 和 Bob 各开启一个事务，分别联系 Blue 和 Green 的服务器检查可用性。Blue 持有"carry_booking_on_monday "这个键值的读锁，Green 持有 "backhoe_booking_on_monday "这个键值的读锁。因为读锁是共享的，所以 Alice 和 Bob 都可以读取这些值。

![检查卡车可用](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/2pc/get_truck_availability.png)
![检查挖土机可用](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/2pc/get_backhoe_availability.png)

Alice 和 Bob 发现这两个预订在星期一都可用。所以，他们向服务器发送 put 请求进行预订。两台服务器都将 put 请求存放在临时存储中。

![预定卡车](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/2pc/reserve_truck.png)
![预定挖掘机](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/2pc/reserve_backhoe.png)

当 Alice 和 Bob 决定提交事务时——假设 Blue 扮演协调者——协调者出发两阶段提交协议，将准备请求发送给自己和 Green。

对于 Alice 的请求，它尝试获取键值'truck_booking_on_monday'的写锁，但它得不到，因为另一个事务得到了冲突的写锁。因此，Alice 的事务在准备阶段就失败了。同样的事情也发生在 Bob 的请求中。

![提交失败](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/2pc/commit_failure_retry.png)

在一个重试循环中，事务不断重试，像下面这样：

```java
class TransactionExecutor…

  public boolean executeWithRetry(Function<TransactionClient, Boolean> txnMethod, ReplicaMapper replicaMapper, SystemClock systemClock) {
      for (int attempt = 1; attempt <= maxRetries; attempt++) {
          TransactionClient client = new TransactionClient(replicaMapper, systemClock);
          try {
              boolean checkPassed = txnMethod.apply(client);
              Boolean successfullyCommitted = client.commit().get();
              return checkPassed && successfullyCommitted;
          } catch (Exception e) {
              logger.error("Write conflict detected while executing." + client.transactionRef + " Retrying attempt " + attempt);
              client.rollback();
              randomWait(); //wait for random interval
          }

      }
      return false;
  }
```

Alice 和 Bob 预订的示例代码如下所示：

```java
class TransactionalKVStoreTest…

  @Test
  public void retryWhenConflict() {
      List<TransactionalKVStore> allServers = createTestServers(WaitPolicy.WoundWait);

      TransactionExecutor aliceTxn = bookTransactionally(allServers, "Alice", new TestClock(1));
      TransactionExecutor bobTxn = bookTransactionally(allServers, "Bob", new TestClock(2));

      TestUtils.waitUntilTrue(() -> (aliceTxn.isSuccess() && !bobTxn.isSuccess()) || (!aliceTxn.isSuccess() && bobTxn.isSuccess()), "waiting for one txn to complete", Duration.ofSeconds(50));
  }

  private TransactionExecutor bookTransactionally(List<TransactionalKVStore> allServers, String user, SystemClock systemClock) {
      List<String> bookingKeys = Arrays.asList("truck_booking_on_monday", "backhoe_booking_on_monday");
      TransactionExecutor t1 = new TransactionExecutor(allServers);
      t1.executeAsyncWithRetry(txnClient -> {
          if (txnClient.isAvailable(bookingKeys)) {
              txnClient.reserve(bookingKeys, user);
              return true;
          }
          return false;
      }, systemClock);
      return t1;
  }
```

在这种情况下，其中一个事务最终会成功，而另一个则会退出。

采用 Error Wait 策略，上面的代码虽然容易实现，但会有多次事务重启，降低了整体的吞吐。正如上节所述，采用 Wound-Wait 可以减少事务重启的次数。在上面的例子中，冲突时只有一个事务可能会重启，而非两个都重启。

### 使用[有版本的值（Versioned Value）](versioned-value.md)

冲突会给所有的读和写操作都带来很大的限制，尤其是当事务是只读的时候。最理想的情况是，在不持有任何锁的情况下，只读的事务可以工作，仍能保证事务中读到的值不会随着并发读写事务而改变。

数据存储一般会存储值的多个版本，就像[有版本的值（Versioned Value）](versioned-value.md)中描述的那样。版本号可以采用遵循 [Lamport 时钟（Lamport Clock）](lamport-clock.md)的时间戳。大多数情况下，像 [MongoDB](https://www.MongoDb.com/) 或 [CockroachDB](https://www.cockroachlabs.com/docs/stable/) 会采用[混合时钟（Hybrid Clock）](hybrid-clock.md)。为了在两阶段提交协议中使用它，诀窍就是每个参与事务的服务器都要发送它可以写入值的时间戳，以此作为对准备请求的响应。协调者会从这些时间戳中选出一个最大值作为提交时间戳，把它和值一起发送。然后，参与服务器把值保存在提交时间戳对应的位置上。这样一来，只读请求就可以在不持有锁的情况下执行，因为它保证了在特定时间戳写入的值是不会改变的。

考虑如下的一个简单示例。Philip 正在运行一份报表，需要读时间戳 2 之前的所有数据。假设这是一个需要持有锁的长时间操作，试图预定卡车的 Alice 就会被阻塞，直到 Philip 的工作完全完成。采用[有版本的值（Versioned Value）](versioned-value.md)，Philip 的 get 请求就是一个只读操作的一部分，它可以在时间戳 2 上继续执行，而 Alice 的预定则在时间戳 4 上继续执行。

![mvcc读](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/2pc/mvcc-read.png)

<center>多版本并发控制读取(MVCC)</center>

需要注意的是，如果读请求是读写事务的一部分，它就依然要持有锁。

采用 [Lamport 时钟（Lamport Clock）](lamport-clock.md)的示例代码如下所示:

```java
class MvccTransactionalKVStore…

  public String readOnlyGet(String key, long readTimestamp) {
      adjustServerTimestamp(readTimestamp);
      return kv.get(new VersionedKey(key, readTimestamp));
  }

  public CompletableFuture<String> get(TransactionRef txn, String key, long readTimestamp) {
      adjustServerTimestamp(readTimestamp);
      CompletableFuture<TransactionRef> lockFuture = lockManager.acquire(txn, key, LockMode.READ);
      return lockFuture.thenApply(transactionRef -> {
          getOrCreateTransactionState(transactionRef);
          return kv.get(key);
      });
  }

  private void adjustServerTimestamp(long readTimestamp) {
      this.timestamp = readTimestamp > this.timestamp ? readTimestamp:timestamp;
  }

  public void put(TransactionRef txnId, String key, String value) {
      timestamp = timestamp + 1;
      TransactionState transactionState = getOrCreateTransactionState(txnId);
      transactionState.addPendingUpdates(key, value);
  }
class MvccTransactionalKVStore…

  private long prepare(TransactionRef txn, Optional<Map<String, String>> pendingUpdates) throws WriteConflictException, IOException {
      if (pendingUpdates.isPresent()) {
          Map<String, String> pendingKVs = pendingUpdates.get();

          acquireLocks(txn, pendingKVs);

          timestamp = timestamp + 1; //increment the timestamp for write operation.

          writeToWAL(txn, pendingKVs, timestamp);
       }
      return timestamp;
  }
class MvccTransactionCoordinator…

  public long commit(TransactionRef txn) {
          long commitTimestamp = prepare(txn);

          TransactionMetadata transactionMetadata = transactions.get(txn);
          transactionMetadata.markPreparedToCommit(commitTimestamp, this.transactionLog);

          sendCommitMessageToAllTheServers(txn, commitTimestamp, transactionMetadata.getParticipatingKeys());

          transactionMetadata.markCommitComplete(transactionLog);

          return commitTimestamp;
  }


  public long prepare(TransactionRef txn) throws WriteConflictException {
      TransactionMetadata transactionMetadata = transactions.get(txn);
      Map<MvccTransactionalKVStore, List<String>> keysToServers = getParticipants(transactionMetadata.getParticipatingKeys());
      List<Long> prepareTimestamps = new ArrayList<>();
      for (MvccTransactionalKVStore store : keysToServers.keySet()) {
          List<String> keys = keysToServers.get(store);
          long prepareTimestamp = store.prepare(txn, keys);
          prepareTimestamps.add(prepareTimestamp);
      }
      return prepareTimestamps.stream().max(Long::compare).orElse(txn.getStartTimestamp());
  }
```

所有参与的集群节点都会在提交时间错的位置上存储键值。

```java
class MvccTransactionalKVStore…

  public void commit(TransactionRef txn, List<String> keys, long commitTimestamp) {
      if (!lockManager.hasLocksFor(txn, keys)) {
          throw new IllegalStateException("Transaction should hold all the required locks");
      }

      adjustServerTimestamp(commitTimestamp);

      applyPendingOperations(txn, commitTimestamp);

      lockManager.release(txn, keys);

      logTransactionMarker(new TransactionMarker(txn, TransactionStatus.COMMITTED, commitTimestamp, keys, Collections.EMPTY_MAP));
  }

  private void applyPendingOperations(TransactionRef txnId, long commitTimestamp) {
      Optional<TransactionState> transactionState = getTransactionState(txnId);
      if (transactionState.isPresent()) {
          TransactionState t = transactionState.get();
          Optional<Map<String, String>> pendingUpdates = t.getPendingUpdates();
          apply(txnId, pendingUpdates, commitTimestamp);
      }
  }

  private void apply(TransactionRef txnId, Optional<Map<String, String>> pendingUpdates, long commitTimestamp) {
      if (pendingUpdates.isPresent()) {
          Map<String, String> pendingKv = pendingUpdates.get();
          apply(pendingKv, commitTimestamp);
      }
      ongoingTransactions.remove(txnId);
  }


  private void apply(Map<String, String> pendingKv, long commitTimestamp) {
      for (String key : pendingKv.keySet()) {
          String value = pendingKv.get(key);
          kv.put(new VersionedKey(key, commitTimestamp), value);
      }
  }
```

#### 技术考量

这里还有一个微妙的问题需要解决。一旦对某个读请求返回了某个给定时间戳的应答，那就不该出现任何比这个时间戳更低的写入。这一点可以通过不同的技术实现。[Google Percolator](https://research.google/pubs/pub36726/) 和受 Percolator 启发的像 [TiKV](https://tikv.org/) 这样的数据存储都会使用一个单独的服务器，称为 Timestamp oracle，它会保证给出单调增长的时间戳。像 [MongoDb](https://www.MongoDb.com/) 或 [CockroachDb](https://www.cockroachlabs.com/docs/stable/) 这样的数据库则采用了[混合时钟（Hybrid Clock）](hybrid-clock.md)，以此保证每个请求都可以将各个服务器上的混合时钟调整到最新。时间戳也会随着每个写入请求增加。最后，提交阶段会从所有参与的服务器中选取最大的时间戳，确保写入总是在前面的读请求之后发生。

值得注意的是，如果客户端读取的时间戳值低于服务器写入的时间戳值，这不是问题。但是，如果客户端正在读取一个时间戳，而服务端正准备在一个特定的时间戳进行写入，那么这就是一个问题了。如果服务端检测到客户端正在读取的一个时间戳，可能存在一个在途的写入（只进行了准备），服务器会拒绝这次写入。对于 [CockroachDB](https://www.cockroachlabs.com/docs/stable/) 而言，如果读取所在的时间戳有一个正在进行的事务，那它就会抛出异常。[Spanner](https://cloud.google.com/spanner) 的读取有一个阶段，客户端会得到某个特定分区上最后一次成功写入的时间。如果客户端在一个较高的时间戳进行读取，那么读取请求就会等待，直到该时间戳的写入完成。

### 使用[复制日志（Replicated Log）](replicated-log.md)

为了提升容错性，集群节点可以使用[复制日志（Replicated Log）](replicated-log.md)。协调者使用复制日志（Replicated Log）存储事务的日志条目。

考虑一下上面一节中的 Alice 和 Bob 的例子，Blue 服务器是一组服务器，Green 服务器也是一组。所有预订数据都会在一组的服务器上进行复制。两阶段提交中的每个请求都会发送给服务组的领导者。复制是通过[复制日志（Replicated Log）](replicated-log.md)实现的。

客户端与每组服务器的领导者通信。只有在客户端决定提交事务时，复制才是必需的，因此，它也发生在预备请求的过程中。

协调者也会将每个状态更改复制到到复制日志中。

在分布式数据存储中，每个集群节点都会处理多个分区。每个分区都会维护一个[复制日志（Replicated Log）](replicated-log.md)。当使用 [Raft](https://raft.github.io/) 时，它有时称为[multi-raft](https://www.cockroachlabs.com/blog/scaling-raft/)。

客户端会与参与事务的每个分区的领导进行通信。

### 失效处理

两段式提交协议很大程度上依赖协调者节点对事务结果的传达。在知晓事务结果之前，单个的集群节点不允许任何其它事务对涉及进行中事务的键值进行写入。集群节点会一直阻塞，直到知晓了事务的结果。这就给协调者提出了一些关键的要求。

即使在进程崩溃的情况下，协调者也要记住事务的状态。

协调者会使用[预写日志（Write-Ahead Log）](write-ahead-log.md)记录每一次的事务状态更新。这样一来，当协调者崩溃再重启之后，它依然能够继续处理未完成的事务。

```java
class TransactionCoordinator…

  public void loadTransactionsFromWAL() throws IOException {
      List<WALEntry> walEntries = this.transactionLog.readAll();
      for (WALEntry walEntry : walEntries) {
          TransactionMetadata txnMetadata = (TransactionMetadata) Command.deserialize(new ByteArrayInputStream(walEntry.getData()));
          transactions.put(txnMetadata.getTxn(), txnMetadata);
      }
      startTransactionTimeoutScheduler();
      completePreparedTransactions();
  }
  private void completePreparedTransactions() throws IOException {
      List<Map.Entry<TransactionRef, TransactionMetadata>> preparedTransactions
              = transactions.entrySet().stream().filter(entry -> entry.getValue().isPrepared()).collect(Collectors.toList());
      for (Map.Entry<TransactionRef, TransactionMetadata> preparedTransaction : preparedTransactions) {
          TransactionMetadata txnMetadata = preparedTransaction.getValue();
          sendCommitMessageToParticipants(txnMetadata.getTxn());
      }
  }
```

在向协调者发送提交消息之前，客户端是可以失败的。

事务协调者会跟踪每个事务状态的更新时间。如果在配置的超时期间内未收到任何状态更新，它就会触发事务回滚 。

```java
class TransactionCoordinator…

  private ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
  private ScheduledFuture<?> taskFuture;
  private long transactionTimeoutMs = Long.MAX_VALUE; //for now.

  public void startTransactionTimeoutScheduler() {
      taskFuture = scheduler.scheduleAtFixedRate(() -> timeoutTransactions(),
              transactionTimeoutMs,
              transactionTimeoutMs,
              TimeUnit.MILLISECONDS);
  }

  private void timeoutTransactions() {
      for (TransactionRef txnRef : transactions.keySet()) {
          TransactionMetadata transactionMetadata = transactions.get(txnRef);
          long now = systemClock.nanoTime();
          if (transactionMetadata.hasTimedOut(now)) {
              sendRollbackMessageToParticipants(transactionMetadata.getTxn());
              transactionMetadata.markRollbackComplete(transactionLog);
          }
      }
  }
```

### 跨异构系统的事务

这里讨论的解决方案展示的是同构系统的两阶段提交。同构，意味着所有的集群节点都是同一系统的一部分，存储这同样类型的数据。比如，像 [MongoDb](https://www.MongoDb.com/) 这样的分布式数据存储，或是像 [Kafka](https://kafka.apache.org/) 这样的分布式消息中间件。

在过去，两阶段提交主要说的是基于异构系统的。两阶段提交最常见的用法是使用 [XA](https://pubs.opengroup.org/onlinepubs/009680699/toc.pdf) 事务。在 J2EE 服务器上，跨消息中间件和数据库进行两阶段提交是很常见的。最常见的使用模式是，在 ActiveMQ 或 JMS 这样的消息中间件产生一条消息时，在数据库中需要插入/更新一条记录。

正如上一节所见，在两阶段提交的实现中，协调者的容错机制扮演了重要的角色。在 XA 事务的场景下，协调者大多数就是进行数据库和消息中间件调用的应用进程。在大多数现代场景中，应用程序就是一个运行在容器化环境中的无状态微服务。这并不是一个真正适合协调者职责的地方。协调器需要维护状态，能够从提交或回滚的失效中快速恢复回来，在这种情况下很难实现。

### 示例

像 [CockroachDb](https://www.cockroachlabs.com/docs/stable/)，[MongoDb](https://www.MongoDb.com/)这样的分布式数据库，利用两阶段提交实现了值的跨分区原子性存储。

[Kafka](https://kafka.apache.org/) 允许跨多分区原子性地生成消息，其实现也类似于两阶段提交。
