# 低水位标记（Low-Water Mark）

**原文**

https://martinfowler.com/articles/patterns-of-distributed-systems/low-watermark.html

预写日志的一个索引，表示日志中的哪个部分是可以丢弃的。

**2020.8.18**

## 问题

预写日志维护着持久化存储的每一次更新。随着时间的推移，它会无限增长。使用[分段日志](segmented-log.md)，一次可以处理更小的文件，但如果不检查，磁盘总存储量会无限增长。

## 解决方案

要有这样一种机制，告诉日志处理部分，哪部分日志可以安全地丢弃了。这种机制要给出最低的偏移或是低水位标记，也就是在这个点之前的日志都可以丢弃了。后台用一个单独的线程执行一个任务，持续检查哪部分日志可以丢弃，然后，从磁盘上删除相应的文件。

```java
this.logCleaner = newLogCleaner(config);
this.logCleaner.startup();
```

日志清理器可以实现成一个调度任务。

```java
public void startup() {
    scheduleLogCleaning();
}
private void scheduleLogCleaning() {
    singleThreadedExecutor.schedule(() -> {
        cleanLogs();
    }, config.getCleanTaskIntervalMs(), TimeUnit.MILLISECONDS);
}
```

### 基于快照的低水位标记

大多数共识算法的实现，比如，Zookeeper 或 etcd（如同 RAFT 中所定义的），都实现了快照机制。在这个实现中，存储引擎会周期地打快照。已经成功应用的日志索引也要和快照一起存起来。可以参考[预写日志（Write-Ahead Log）](write-ahead-log.md)模式中的简单键值存储的实现，快照可以像下面这样打：

```java
public SnapShot takeSnapshot() {
    Long snapShotTakenAtLogIndex = wal.getLastLogEntryId();
    return new SnapShot(serializeState(kv), snapShotTakenAtLogIndex);
}
```

快照一旦持久化到磁盘上，日志管理器就会得到低水位标记，之后，就可以丢弃旧的日志了。

```java
List<WALSegment> getSegmentsBefore(Long snapshotIndex) {
    List<WALSegment> markedForDeletion = new ArrayList<>();
    List<WALSegment> sortedSavedSegments = wal.sortedSavedSegments;
    for (WALSegment sortedSavedSegment : sortedSavedSegments) {
        if (sortedSavedSegment.getLastLogEntryId() < snapshotIndex) {
            markedForDeletion.add(sortedSavedSegment);
        }
    }
    return markedForDeletion;
}
```

### 基于时间的低水位标记

在一些系统中，日志并不是更新系统状态所必需的，在给定的时间窗口后，日志就可以丢弃了，而无需等待其它子系统将可以删除的最低的日志索引共享过来。比如，像 Kafka 这样的系统里，日志维持七周；消息大于七周的日志段都可以丢弃。就这个实现而言，日志条目也包含了其创建的时间戳。这样，日志清理器只要检查每个日志段的最后一项，如果其在配置的时间窗口之前，这个段就可以丢弃了。

```java
private List<WALSegment> getSegmentsPast(Long logMaxDurationMs) {
    long now = System.currentTimeMillis();
    List<WALSegment> markedForDeletion = new ArrayList<>();
    List<WALSegment> sortedSavedSegments = wal.sortedSavedSegments;
    for (WALSegment sortedSavedSegment : sortedSavedSegments) {
        if (timeElaspedSince(now, sortedSavedSegment.getLastLogEntryTimestamp()) > logMaxDurationMs) {
            markedForDeletion.add(sortedSavedSegment);
        }
    }
    return markedForDeletion;
}

private long timeElaspedSince(long now, long lastLogEntryTimestamp) {
    return now - lastLogEntryTimestamp;
}
```

## 示例

* 所有共识算法的日志实现，比如 [Zookeeper](https://github.com/apache/zookeeper/blob/master/zookeeper-server/src/main/java/org/apache/zookeeper/server/persistence/FileTxnLog.java) 和 [RAFT](https://github.com/etcd-io/etcd/blob/master/wal/wal.go)，都实现基于快照的日志清理。
* [Kafka](https://github.com/axbaretto/kafka/blob/master/core/src/main/scala/kafka/log/Log.scala) 的存储实现遵循着基于时间的日志清理。
