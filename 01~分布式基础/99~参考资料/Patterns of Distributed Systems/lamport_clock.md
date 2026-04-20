# Lamport 时钟（Lamport Clock）

**原文**

https://martinfowler.com/articles/patterns-of-distributed-systems/lamport-clock.html

使用逻辑时间戳作为一个值的版本，以便支持跨服务器的值排序。

**2021.6.23**

## 问题

当值要在多个服务器上进行存储时，需要有一种方式知道一个值要在另外一个值之前存储。在这种情况下，不能使用系统时间戳，因为时钟不是单调的，两个服务器的时钟时间不应该进行比较。

表示一天中时间的系统时间戳，一般来说是通过晶体振荡器建造的时钟机械测量的。这种机制有一个已知问题，根据晶体震荡的快慢，它可能会偏离一天实际的时间。为了解决这个问题，计算机通常会使用像 NTP 这样的服务，将计算机时钟与互联网上众所周知的时间源进行同步。正因为如此，在一个给定的服务器上连续读取两次系统时间，可能会出现时间倒退的现象。

由于服务器之间的时钟漂移没有上限，比较两个不同的服务器的时间戳是不可能的。

## 解决方案

Lamport 时钟维护着一个单独的数字表示时间戳，如下所示：

```java
class LamportClock…

  class LamportClock {
      int latestTime;

      public LamportClock(int timestamp) {
          latestTime = timestamp;
      }
```

每个集群节点都维护着一个 Lamport 时钟的实例。

```java
class Server…

  MVCCStore mvccStore;
  LamportClock clock;

  public Server(MVCCStore mvccStore) {
      this.clock = new LamportClock(1);
      this.mvccStore = mvccStore;
  }
```

服务器每当进行任何写操作时，它都应该使用`tick()`方法让 Lamport 时钟前进。

```java
class LamportClock…

  public int tick(int requestTime) {
      latestTime = Integer.max(latestTime, requestTime);
      latestTime++;
      return latestTime;
  }
```

如此一来，服务器可以确保写操作的顺序是在这个请求之后，以及客户端发起请求时服务器端已经执行的任何其他动作之后。服务器会返回一个时间戳，用于将值写回给客户端。稍后，请求的客户端会使用这个时间戳向其它的服务器发起进一步的写操作。如此一来，请求的因果链就得到了维持。

### 因果性、时间和 Happens-Before

在一个系统中，当一个事件 A 发生在事件 B 之前，这其中可能存在因果关系。因果关系意味着，在导致 B 发生的原因中，A 可能扮演了一些角色。这种“A 发生在 B 之前（A happens before B）”的关系是通过在每个事件上附加时间戳达成的。如果 A 发生在 B 之前，附加在 A 的时间戳就会小于附加在 B 上的时间戳。但是，因为我们无法依赖于系统时间，我们需要一些方式确保这种“依赖于附加在事件上的时间戳”的 Happens-Before 关系得到维系。[Leslie Lamport](https://en.wikipedia.org/wiki/Leslie_Lamport) 在其开创性论文[《时间、时钟和事件排序（Time, Clocks and Ordering Of Events）》](https://lamport.azurewebsites.net/pubs/time-clocks.pdf)中提出了一个解决方案，使用逻辑时间戳来跟踪 Happens-Before 的关系。因此，这种使用逻辑时间错追踪因果性的技术就被称为 Lamport 时间戳。

值得注意的是，在数据库中，事件是关于存储数据的。因此，Lamport 时间戳会附加到存储的值上。这非常符合有版本的存储机制，这一点我们在[有版本的值（Versioned Value）](versioned-value.md)中讨论过。

### 一个样例键值存储

考虑一个有多台服务器节点的简单键值存储的例子。它包含两台服务器，蓝色（Blue）和绿色（Green）。每台服务器负责存储一组特定的键值。这是一个典型的场景，数据划分到一组服务器上。值存储为[有版本的值（Versioned Value）](versioned-value.md)，其版本号为 Lamport 时间戳。

![两台服务器，各自负责特定的键值](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/two-servers-each-with-specific-key-range.png)

<center>图1：两台服务器，各自负责特定的键值</center>

接收服务器会比较并更新自己的时间戳，然后，用它写入一个有版本的键值和值。

```java
class Server…

  public int write(String key, String value, int requestTimestamp) {
      //update own clock to reflect causality
      int writeAtTimestamp = clock.tick(requestTimestamp);
      mvccStore.put(new VersionedKey(key, writeAtTimestamp), value);
      return writeAtTimestamp;
  }
```

用于写入值的时间戳会返回给客户端。通过更新自己的时间戳，客户端会跟踪最大的时间戳。它在发出进一步写入请求时会使用这个时间戳。

```java
class Client…

  LamportClock clock = new LamportClock(1);
  public void write() {
      int server1WrittenAt = server1.write("name", "Alice", clock.getLatestTime());
      clock.updateTo(server1WrittenAt);

      int server2WrittenAt = server2.write("title", "Microservices", clock.getLatestTime());
      clock.updateTo(server2WrittenAt);

      assertTrue(server2WrittenAt > server1WrittenAt);
  }
```

请求序列看起来是下面这样：

![两台服务器，各自负责特定的键值](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/lamport-clock-request-sequence.png)

<center>图2：两台服务器，各自负责特定的键值</center>

在[领导者和追随者（Leader and Followers）](leader-and-followers.md)组中，甚至可以用同样的技术在客户端和领导者之间的通信，每组负责一组特定的键值。客户端向该组的领导者发送请求，如上所述。Lamport 时钟的实例由该组的领导者维护，其更新方式与上一节讨论的完全相同。

![不同的领导者追随者组存储不同的键值](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/different-keys-different-servers.png)

<center>图3：不同的领导者追随者组存储不同的键值</center>

### 部分有序

使用 Lamport 时钟存储的值只能是[部分有序的](https://en.wikipedia.org/wiki/Partially_ordered_set)。如果两个客户端在两台单独的服务器上存储值，时间戳的值是不能用于跨服务器进行值排序的。在下面这个例子里，Bob 在绿色服务器上存储的标题，其时间戳是 2。但是，这并不能决定 Bob 存储的标题是在 Alice 在蓝色服务器存储名字之前还是之后。

![部分有序](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/two-clients-two-separate-servers.png)

<center>图4：部分有序</center>

### 单一服务器/领导者更新值

对一个领导者追随者服务器组而言，领导者总是负责存储值，其基本实现已经在[有版本的值（Versioned Value）](versioned-value.md)中讨论过，它足以维持所需的因果性。

![单一领导者追随者组进行键值存储](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/single-servergroup-kvstore.png)

<center>图 5：单一领导者追随者组进行键值存储</center>

在这种情况下，键值存储会保持一个整数的版本计数器。每次从预写日志中应用了写入命令，版本计数器就要递增。然后，用递增过的版本计数器构建一个新的键值。只有领导者负责递增版本计数器，追随者使用相同的版本号。

```java
class ReplicatedKVStore…

  int version = 0;
  MVCCStore mvccStore = new MVCCStore();

  @Override
  public CompletableFuture<Response> put(String key, String value) {
      return server.propose(new SetValueCommand(key, value));
  }

  private Response applySetValueCommand(SetValueCommand setValueCommand) {
      getLogger().info("Setting key value " + setValueCommand);
      version = version + 1;
      mvccStore.put(new VersionedKey(setValueCommand.getKey(), version), setValueCommand.getValue());
      Response response = Response.success(version);
      return response;
  }
```

## 示例

像 [mongodb](https://www.mongodb.com/) 和 [cockroachdb](https://www.cockroachlabs.com/docs/stable/) 采用了 Lamport 时钟的变体实现了 [mvcc](https://en.wikipedia.org/wiki/Multiversion_concurrency_control) 存储。

[世代时钟（Generation Clock）](generation-clock.md)是 Lamport 时钟的一个例子。
