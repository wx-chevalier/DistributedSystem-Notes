# 分段日志（Segmented Log）

**原文**

https://martinfowler.com/articles/patterns-of-distributed-systems/log-segmentation.html

将日志分成多个文件，不能只为了操作简单而使用一个大文件。

**2020.8.13**

## 问题

单个日志文件会增大，在启动时读取它，会变成性能瓶颈。旧的日志要定期清理，对一个巨大的文件做清理操作是很难实现的。

## 解决方案

单个日志分成多个段。到达指定规模的上限之后，日志文件会滚动。

```java
public Long writeEntry(WALEntry entry) {
    maybeRoll();
    return openSegment.writeEntry(entry);
}

private void maybeRoll() {
    if (openSegment.
            size() >= config.getMaxLogSize()) {
        openSegment.flush();
        sortedSavedSegments.add(openSegment);
        long lastId = openSegment.getLastLogEntryId();
        openSegment = WALSegment.open(lastId, config.getWalDir());
    }
}
```

有了日志分段，还要有一种简单的方式将逻辑日志偏移（或是日志序列号）同日志分段文件做一个映射。实现这一点可以通过下面两种方式：

* 每个日志分段名都是生成的，可以采用众所周知的前缀加基本偏移（日志序列号）的方式。
* 每个日志序列分成两个部分，文件名和事务偏移量。

```java
public static String createFileName(Long startIndex) {
    return logPrefix + "_" + startIndex + logSuffix;
}

public static Long getBaseOffsetFromFileName(String fileName) {
    String[] nameAndSuffix = fileName.split(logSuffix);
    String[] prefixAndOffset = nameAndSuffix[0].split("_");
    if (prefixAndOffset[0].equals(logPrefix))
        return Long.parseLong(prefixAndOffset[1]);

    return -1l;
}
```

有了这些信息，读操作要有两步。对于给定的偏移（或是事务 ID），确定日志分段，从后续的日志段中读取所有的日志记录。

```java
public List<WALEntry> readFrom(Long startIndex) {
    List<WALSegment> segments = getAllSegmentsContainingLogGreaterThan(startIndex);
    return readWalEntriesFrom(startIndex, segments);
}

private List<WALSegment> getAllSegmentsContainingLogGreaterThan(Long startIndex) {
    List<WALSegment> segments = new ArrayList<>();
    //Start from the last segment to the first segment with starting offset less than startIndex
    //This will get all the segments which have log entries more than the startIndex
    for (int i = sortedSavedSegments.size() - 1; i >= 0; i--) {
        WALSegment walSegment = sortedSavedSegments.get(i);
        segments.add(walSegment);
        if (walSegment.getBaseOffset() <= startIndex) {
            break; // break for the first segment with baseoffset less than startIndex
        }
    }

    if (openSegment.getBaseOffset() <= startIndex) {
        segments.add(openSegment);
    }

    return segments;
}
```

## 示例

* 所有的共识实现都使用了日志分段，比如，[Zookeeper](https://github.com/apache/zookeeper/blob/master/zookeeper-server/src/main/java/org/apache/zookeeper/server/persistence/FileTxnLog.java) 和 [RAFT](https://github.com/etcd-io/etcd/blob/master/wal/wal.go)。
* [Kafka](https://github.com/axbaretto/kafka/blob/master/core/src/main/scala/kafka/log/Log.scala) 的存储实现也遵循日志分段。
* 所有的数据库，包括 NoSQL 数据库，类似于 [Cassandra](https://github.com/facebookarchive/cassandra/blob/master/src/org/apache/cassandra/db/CommitLog.java)，都使用基于预先配置日志大小的滚动策略。
