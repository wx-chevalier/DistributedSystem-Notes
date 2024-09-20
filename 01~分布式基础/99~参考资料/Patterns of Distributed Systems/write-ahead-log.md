# 预写日志（Write-Ahead Log）

**原文**

https://martinfowler.com/articles/patterns-of-distributed-systems/wal.html

将每个状态变化以命令形式持久化至只追加的日志中，提供持久化保证，而无需以存储数据结构存储到磁盘上。

**2020.8.12**

也称：提交日志

## 问题

对服务器而言，即便在机器存储数据失败的情况下，也要保证强持久化，这一点是必需的。一旦服务器同意执行某个动作，即便服务器失效，重启后丢失了所有内存状态，它也应该做到保证执行。

## 解决方案

![预写日志](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/wal.png)

<center>图1：预写日志</center>

将每个状态变化以命令的形式存储在磁盘文件中。一个日志由一个服务端进程维护，其只进行顺序追加。一个顺序追加的日志，简化了重启时以及后续在线操作（新命令追加至日志时）的日志处理。每个日志条目都有一个唯一的标识符。唯一的日志标识符有助于在日志中实现某些其它操作，比如[分段日志（Segmented Log）](segmented-log.md)，或是以[低水位标记（Low-Water Mark）](low-water-mark.md)清理日志等等。日志的更新可以通过[单一更新队列（Singular Update Queue）](singular-update-queue.md)实现。

典型的日志条目结构如下所示：

```java
class WALEntry…
  private final Long entryId;
  private final byte[] data;
  private final EntryType entryType;
  private long timeStamp;
```

每次重启时，可以读取这个文件，然后，重放所有的日志条目就可以恢复状态。

考虑下面这个简单的内存键值存储：

```java
class KVStore…
  private Map<String, String> kv = new HashMap<>();
  public String get(String key) {
      return kv.get(key);
  }
  public void put(String key, String value) {
      appendLog(key, value);
      kv.put(key, value);
  }
  private Long appendLog(String key, String value) {
      return wal.writeEntry(new SetValueCommand(key, value).serialize());
  }
```

put 操作表示成了命令（Command），在更新内存的哈希表 之前先把它序列化，然后存储到日志里。

```java
class SetValueCommand…
  final String key;
  final String value;
  final String attachLease;

  public SetValueCommand(String key, String value) {
      this(key, value, "");
  }

  public SetValueCommand(String key, String value, String attachLease) {
      this.key = key;
      this.value = value;
      this.attachLease = attachLease;
  }

  @Override
  public void serialize(DataOutputStream os) throws IOException {
      os.writeInt(Command.SetValueType);
      os.writeUTF(key);
      os.writeUTF(value);
      os.writeUTF(attachLease);
  }

  public static SetValueCommand deserialize(InputStream is) {
      try {
          DataInputStream dataInputStream = new DataInputStream(is);
          return new SetValueCommand(dataInputStream.readUTF(), dataInputStream.readUTF(), dataInputStream.readUTF());
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
  }
```

这就保证了一旦 put 方法返回成功，即便负责 KVStore 的进程崩溃了，启动时，通过读取日志文件也可以将状态恢复回来。

```java
class KVStore…
  public KVStore(Config config) {
      this.config = config;
      this.wal = WriteAheadLog.openWAL(config);
      this.applyLog();
  }

  public void applyLog() {
      List<WALEntry> walEntries = wal.readAll();
      applyEntries(walEntries);
  }

  private void applyEntries(List<WALEntry> walEntries) {
      for (WALEntry walEntry : walEntries) {
          Command command = deserialize(walEntry);
          if (command instanceof SetValueCommand) {
              SetValueCommand setValueCommand = (SetValueCommand)command;
              kv.put(setValueCommand.key, setValueCommand.value);
          }
      }
  }

  public void initialiseFromSnapshot(SnapShot snapShot) {
      kv.putAll(snapShot.deserializeState());
  }
```

### 实现考量

实现日志还有一些重要的考量。有一点很重要，就是确保写入日志文件的日志条目已经实际地持久化到物理介质上。所有程序设计语言的文件处理程序库都提供了一个机制，强制操作系统将文件的变化“刷（flush）”到物理介质上。然而，使用这种机制有一个需要考量的权衡点。

将每个日志的写入都刷到磁盘，这种做法是给了我们一种强持久化的保证（这是使用日志的首要目的），但是，这种做法严重限制了性能，可能很快就会变成瓶颈。将刷的动作延迟，或是采用异步处理，性能就可以提高，但服务器崩溃时，日志条目没有刷到磁盘上，就存在丢失日志条目的风险。大多数采用的技术类似于批处理，以此限制刷操作的影响。

还有一个考量，就是如果日志文件受损，读取日志的时候，要保证能够检测出来，日志条目一般来说都会采用 CRC 的方式记录，这样，读取文件时可以对其进行校验。

单独的日志文件可能会变得难于管理，可能会快速地消耗掉所有的存储。为了处理这个问题，可以采用像[分段日志（Segmented Log）](segmented-log.md)和[低水位标记（Low-Water Mark）](low-water-mark.md)这样的技术。

预写日志是只追加的。因为这种行为，在客户端通信失败重试的时候，日志可能会包含重复的条目。应用这些日志条目时，需要确保重复可以忽略。如果最终状态是类似于 HashMap 的东西，也就是对同一个键值的更新是幂等的，那就不需要特殊的机制。否则，就需要一些机制，对于有唯一标识符的每个请求进行标记，检测重复。

## 示例

- 所有类似于 [Zookeeper](https://github.com/apache/zookeeper/blob/master/zookeeper-server/src/main/java/org/apache/zookeeper/server/persistence/FileTxnLog.java) 和 [RAFT](https://github.com/etcd-io/etcd/blob/master/server/wal/wal.go) 的共识算法中的日志实现都类似于预写日志。
- [Kakfa](https://github.com/axbaretto/kafka/blob/master/core/src/main/scala/kafka/log/Log.scala) 的存储实现遵循着类似于数据库提交日志的结构。
- 所有的数据库，包括类似于 Cassandra 这样的 NoSQL 数据库都使用[预写日志技术](https://github.com/apache/cassandra/blob/trunk/src/java/org/apache/cassandra/db/commitlog/CommitLog.java)保证了持久性。
