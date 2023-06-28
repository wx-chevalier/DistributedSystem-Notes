# Paxos

**原文**

https://martinfowler.com/articles/patterns-of-distributed-systems/paxos.html

采用两阶段共识构建，即便节点断开连接，也能安全地达成共识。

**2021.1.5**

## 问题

当多个节点共享状态时，它们往往需要彼此之间就对某个特定值达成一致。采用[领导者和追随者（Leader and Followers）](leader-and-followers.md)模式，领导者会确定这个值，并将其传递给追随者。但是，如果没有领导者，这些节点就需要自己确定一个值。（即便采用了领导者和追随者，它们也需要这么做选举出一个领导者。）

通过采用[两阶段提交（Two Phase Commit）](two-phase-commit.md)，领导者可以确保副本安全地获得更新，但是，如果没有领导者，我们可以让竞争的节点尝试获取 [Quorum](quorum.md)。这个过程更加复杂，因为任何节点都可能会失效或断开连接。一个节点可能会在一个值上得到 Quorum，但在将这个值传给整个集群之前，它就断开连接了。

## 解决方案

Paxos 算法由 [Leslie Lamport](http://lamport.org/) 开发，发表于 1998 年的论文[《The Part-Time Parliament》](http://lamport.azurewebsites.net/pubs/pubs.html#lamport-paxos)中。Paxos 的工作分为三个阶段，以确保即便在部分网络或节点失效的情况下，多个节点仍能对同一值达成一致。前两个阶段的工作是围绕一个值构建共识，最后一个阶段是将该共识传达给其余的副本。

- 准备阶段：建立最新的[世代时钟（Generation Clock）](generation-clock.md)，收集已经接受的值。
- 接受阶段：提出该世代的值，让各副本接受。
- 提交阶段：让所有的副本了解已经选择的这个值。

在第一阶段（称为**准备阶段**），提出值的节点（称为**提议者**）会联系集群中的所有节点（称为**接受者**），它会询问他们是否能承诺（promise）考虑它给出的值。一旦接受者形成一个 Quorum，都返回其承诺（promise），提议者就会进入下一个阶段。在第二个阶段中（称为**接受阶段**），提议者会发出提议的值，如果节点的 Quorum 接受了这个值，那这个值就**被选中**了。在最后一个阶段（称为**提交阶段**），提议者就会把这个选中的值提交到集群的所有节点上。

### 协议流程

Paxos 是一个难于理解的协议。我们先从一个展示协议典型流程的例子开始，然后再深入到其工作细节之中。我们想通过这个解释提供一个对协议工作原理的直观感受，而非一个全面的描述当做某个实现的基础。

下面是协议的极简概述。

| 提议者                                                                                                                                                                                   | 接受者                                                                                                                                     |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| 从[世代时钟](generation-clock.md)获取下一个世代号。向所有的接受者发送带有该世代值的准备请求。                                                                                            |                                                                                                                                            |
|                                                                                                                                                                                          | 如果准备请求的世代号晚于其承诺的世代变量，它会用这个比较晚的值更新其承诺的世代，并返回一个承诺应答。如果它接受了这个提议，则返回这个提议。 |
| 收到来自接受者 Quorum 的承诺时，它会查看这些应答中包含已接受的值。如果是这样，它就把自己的提议值改成返回的具有最高世代号的提议值。向所有接受者发送接受请求，并附上它的世代号以及提议值。 |
|                                                                                                                                                                                          | 如果接受请求的世代号晚于其承诺的世代变量，它会将提议存储起来，作为其接受的提议，并应答说已经接受了该请求。                                 |
| 收到来自接受者 Quorum 的成功响应时，它将该值记录为选中，并向所有节点发送提交消息。                                                                                                       |                                                                                                                                            |

这些是 paxos 的基本规则，但想要理解它们是如何组成一个有效行为却是非常困难的。因此，这里会用一个说明它是如何工作的。

![](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-initial-requests.png)

考虑一个有五个节点的集群：雅典（Athens）、拜占庭（Byzantium）、锡兰（Cyrene）、德尔菲（Delphi）和以弗所（Ephesus）。一个客户端联系雅典（Athens）节点，请求将名字设置为“alice”。雅典（Athens）现在需要发起一个 Paxos 交互，看是否所有节点都同意这个变化。雅典（Athens）称为提议者，因为在这个过程中，雅典（Athens）向所有其它节点建议将集群中的名字改成“alice”。集群中的所有节点（包括雅典（Athens）自身）都是“接受者”，这意味着它们能够接受提议。

在雅典（Athens）提议“alice”的同时，以弗所（Ephesus）节点也得到了一个请求，将名字设置为“elanor”。这让以弗所（Ephesus）也成为了一个提议者。

[](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-initial-prepare.png)

在准备阶段，提议者首先发送一些准备请求，这些请求都包括一个世代数。由于 Paxos 旨在避免单点故障，我们不会从单一的世代时钟中获取这个数字。相反，每个节点都维护着自己的世代时钟，它将生成号码与节点 ID 相结合。节点 ID 被用来打破平局，所以，[2,a] > [1,e] > [1,a]。每个接受者都记录着它到目前为止所见的最新承诺。

| 节点       | 雅典（Athens） | 拜占庭（Byzantium） | 锡兰（Cyrene） | 德尔菲（Delphi） | 以弗所（Ephesus） |
| ---------- | -------------- | ------------------- | -------------- | ---------------- | ----------------- |
| 承诺的世代 | 1,a            | 1,a                 | 0              | 1,e              | 1,e               |
| 接受的值   | 无             | 无                  | 无             | 无               | 无                |

由于它们在此之前没有见过任何请求，所以，它们都会向调用的提议者返回一个承诺。我们将返回的值称为“承诺”，因为它表明接受者承诺不考虑任何世代时钟早于已承诺的消息。

[](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-a-prepare-c.png)

雅典（Athens）将准备好的信息发送给锡兰（Cyrene）。当它收到一个返回的承诺时，这意味着它现在已经得到了五个节点中三个节点的承诺，这表示达成了一个 [Quorum](quorum.md)。雅典（Athens）现在就从发送准备信息切换为发送接受信息。

有可能雅典（Athens）未能收到大多数集群节点的承诺。在这种情况下，雅典（Athens）可以通过递增世代时钟的方式对准备请求进行重试。

| 节点       | 雅典（Athens） | 拜占庭（Byzantium） | 锡兰（Cyrene） | 德尔菲（Delphi） | 以弗所（Ephesus） |
| ---------- | -------------- | ------------------- | -------------- | ---------------- | ----------------- |
| 承诺的世代 | 1,a            | 1,a                 | 1,a            | 1,e              | 1,e               |
| 接受的值   | 无             | 无                  | 无             | 无               | 无                |

[](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-accept-ac.png)

雅典（Athens）现在开始发送接受信息，其中包含世代以及提议的值。雅典（Athens）和拜占庭（Byzantium）接受了该提议。

| 节点       | 雅典（Athens） | 拜占庭（Byzantium） | 锡兰（Cyrene） | 德尔菲（Delphi） | 以弗所（Ephesus） |
| ---------- | -------------- | ------------------- | -------------- | ---------------- | ----------------- |
| 承诺的世代 | 1,a            | 1,a                 | 1,a            | 1,e              | 1,e               |
| 接受的值   | alice          | alice               | 无             | 无               | 无                |

[](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-e-prepare-c.png)

以弗所（Ephesus）现在向锡兰（Cyrene）发出了一个准备信息。锡兰（Cyrene）曾向雅典（Athens）发出一次承诺，但以弗所（Ephesus）的请求有着更高的世代，所以它优先。锡兰（Cyrene）向以弗所（Ephesus）发回了一个承诺。

[](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-c-refuses-a.png!)

锡兰（Cyrene）现在接收到雅典（Athens）的接受请求，但却拒绝了它，因为其世代数已经落后于它对以弗所（Ephesus）的承诺。

| 节点       | 雅典（Athens） | 拜占庭（Byzantium） | 锡兰（Cyrene） | 德尔菲（Delphi） | 以弗所（Ephesus） |
| ---------- | -------------- | ------------------- | -------------- | ---------------- | ----------------- |
| 承诺的世代 | 1,a            | 1,a                 | 1,e            | 1,e              | 1,e               |
| 接受的值   | alice          | alice               | 无             | 无               | 无                |

[](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-e-send-accepts.png)

现在，以弗所（Ephesus）已经从它的准备消息中得到了一个 Quorum，所以，它可以继续发送接受消息了。它向自己与德尔菲（Delphi）发送了接受消息，但是，在它发送更多的接受消息之前，它崩溃了。

| 节点       | 雅典（Athens） | 拜占庭（Byzantium） | 锡兰（Cyrene） | 德尔菲（Delphi） | 以弗所（Ephesus） |
| ---------- | -------------- | ------------------- | -------------- | ---------------- | ----------------- |
| 承诺的世代 | 1,a            | 1,a                 | 1,e            | 1,e              | 1,e               |
| 接受的值   | alice          | alice               | 无             | elanor           | elanor            |

与此同时，雅典（Athens）必须处理其接受请求被锡兰（Cyrene）拒绝的问题。这表明它的 Quorum 不再能够给予它承诺了，因此，其提议会失败。一个提议者像这样失去最初的 Quorum，这种情况就会发生；另一个提议者要取得 Quorum，第一个提议者的 Quorum 中至少要有一个成员叛变。

在一个简单的两阶段提交的情况下，我们会期望以弗所（Ephesus）继续执行下去，让它的值得到选择，这个模式会有问题，因为以弗所（Ephesus）已经崩溃了。如果它拥有了接受者 Quorum 的锁，它的崩溃会让整个提议过程陷入死锁。然而，Paxos 预计到这种事情会发生，因此，雅典（Athens）会再进行一次尝试，这次它会采用一个更高的世代数。

[](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-a-2nd-prep.png)

它会再次发送准备消息，但是这次的世代数会更高。同第一轮一样，它依然会得到三组承诺，但会有一个重要的区别。雅典（Athens）之前已经接受了“alice”，德尔菲（Delphi）已经接受了“elanor”。这两个接受者都返回了承诺，而且还返回了它们已经接受的值，以及它们所接受提议的世代数。在返回这个值的时候，它们会更新其承诺的世代，也就变成了[2,a]，这样就可以反映它们对雅典（Athens）所做的承诺。

| 节点       | 雅典（Athens） | 拜占庭（Byzantium） | 锡兰（Cyrene） | 德尔菲（Delphi） | 以弗所（Ephesus） |
| ---------- | -------------- | ------------------- | -------------- | ---------------- | ----------------- |
| 承诺的世代 | 2,a            | 1,a                 | 2,a            | 2,a              | 1,e               |
| 接受的值   | alice          | alice               | 无             | elanor           | elanor            |

拥有了 Quorum 的雅典（Athens）现在必须进入到接受阶段，但它提议拥有最高世代的已接受值，也就是“elanor”，这是德尔菲（Delphi）所接受的，其世代为[1,e]，它大于雅典（Athens）接受的“alice”，其世代为[1,a]。

[](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-a-2nd-accept.png)

雅典（Athens）开始发送接受请求，但是，现在发出的是“elanor”及其当前世代。雅典（Athens）给自己发了一个接受请求，这会得到接受。这是一个关键的接受，因为现在有三个节点接受了“elanor”，也就是说，“elanor”达到了 Quorum，因此，我们可以认为“elanor”成为了选中的值。

| 节点       | 雅典（Athens） | 拜占庭（Byzantium） | 锡兰（Cyrene） | 德尔菲（Delphi） | 以弗所（Ephesus） |
| ---------- | -------------- | ------------------- | -------------- | ---------------- | ----------------- |
| 承诺的世代 | 2,a            | 1,a                 | 2,a            | 2,a              | 1,e               |
| 接受的值   | elanor         | alice               | 无             | elanor           | elanor            |

但是，尽管“elanor”现已成为选中的值，但没人知道这一点。在接受阶段，雅典（Athens）只知道自己有“elanor”这个值，这不是一个 Quorum，而且以弗所（Ephesus）已经下线了。雅典（Athens）需要做的就是再接受到几个接受请求，它就可以提交了。但此时，雅典（Athens）崩溃了。

在这个时点上，雅典（Athens）和以弗所（Ephesus）此刻都已经崩溃了。但是集群仍然有一个节点的 Quorum 在运行，所以，它们应该能够继续工作，事实上，通过遵循协议，他们可以发现 “elanor”是选中的值。

[](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-c-prepare.png)

锡兰（Cyrene）接收到一个请求，将名字设置为“carol”，因此，它变成了一个提议者。它看到了[2,a]这个世代，所以，它会启动[3,c]这个世代的准备阶段。虽然它希望提议用“carol”作为名字，但当前它只是发出了准备请求。

锡兰（Cyrene）向集群中的其余节点发送准备信息。与雅典（Athens）之前的准备阶段一样，锡兰（Cyrene）会得到已接受的值，所以，“carol”不会得到提议的机会。同之前一样，德尔菲（Delphi）的“elanor”比拜占庭（Byzantium）的“alice”晚，所以，锡兰（Cyrene）会用 “elanor”和[3,c]开启一个接受阶段。

| 节点       | 雅典（Athens） | 拜占庭（Byzantium） | 锡兰（Cyrene） | 德尔菲（Delphi） | 以弗所（Ephesus） |
| ---------- | -------------- | ------------------- | -------------- | ---------------- | ----------------- |
| 承诺的世代 | 2,a            | 3,c                 | 3,c            | 3,c              | 1,e               |
| 接受的值   | elanor         | alice               | 无             | elanor           | elanor            |

[](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-c-accept.png)

虽然我还可以继续崩溃和唤醒节点，但现在很明显，“elanor”将赢得胜利。只要有节点的 Quorum 在运行，其中至少有一个节点的值是 “elanor”，任何试图进行准备的节点都必须联系一个接受了“elanor”的节点，以便在准备阶段获得一个 Quorum。因此，我们将以 锡兰（Cyrene）发出提交结束这个讨论。

[](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/mfpaxos-c-commit.png)

在某些时点，雅典（Athens）和以弗所（Ephesus）会重新上线，它们会发现 Quorum 的选择。

### 请求无需拒绝

在上面的例子中，我们看到接受者拒绝了世代较老的请求。但是，协议并不要求像这样明确地拒绝。按照规定，接受者可以直接忽略一个过期的请求。如果是这种情况，那么，协议仍然可以收敛在一个共识的值上。这是协议的一个重要特征，因为这是一个分布式系统，连接在任何时候都可能会丢失，所以，不依赖拒绝，对于确保协议安全而言，是有益的。（这里的安全意味着协议将会选择唯一的一个值，一旦选择，就不会改写）。

然而，发送拒绝书仍然是有用的，因为它可以提高性能。提议者越快地发现它们已经老了，它们就能越快开始另一轮更高的世代。

### 竞争的提议者可能无法选择

这个协议可能出错的一种方式是，两个（或更多）提议者进入了一个循环。

- 雅典（Athens）和拜占庭（Byzantium）接受了 alice。
- 所有节点都为 elanor 做了准备，这阻止 alice 获得 Quorum。
- 德尔菲（Delphi）和以弗所（Ephesus）接受了 elanor。
- 所有节点都为 alice 做了准备，这阻止 elanor 获得 Quorum。
- 雅典（Athens）和拜占庭（Byzantium）接受了 alice。

...以此类推，这种情况称为活锁（livelock）。

[FLP 的不可能性结果（FLP Impossibility Result）](https://groups.csail.mit.edu/tds/papers/Lynch/jacm85.pdf)显示，即使只有一个有问题的节点，这也能阻止整个集群选出一个值。

每当一个提议者需要选择一个新的世代时，它必须等待一段随机的时间，我们可以以此确保减少这种活锁发生的机会。一个提议者在另一个提议者向全部 Quorum 发起准备请求之前，就让一个 Quorum 得到接受，这种随机性就让这种情况成为了可能。

但我们永远无法杜绝活锁的发生。这是一个基本的权衡：要么确保安全，要么确保活锁，二者不能得兼。Paxos 首先确保安全。

### 一个样例的键值存储

这里解释的 Paxos 协议，构建的是对于单一值的共识（通常称为单一 Paxos）。大多数主流产品（如 [Cosmos DB](https://docs.microsoft.com/en-us/azure/cosmos-db/introduction) 或 [Spanner](https://cloud.google.com/spanner)）中使用的实际实现都是对 Paxos 进行了修改，称为多重 paxos，其实现方式为 [复制日志（Replicated Log）](replicated-log.md)。

但是，一个简单的键值存储可以使用基本的 Paxos 进行构建。[cassandra](http://cassandra.apache.org/) 以类似的方式使用基本 Paxos 实现了其轻量级的事务。

键值存储为每个键值维护了一个 Paxos 实例。

```java
class PaxosPerKeyStore…

  int serverId;
  public PaxosPerKeyStore(int serverId) {
      this.serverId = serverId;
  }

  Map<String, Acceptor> key2Acceptors = new HashMap<String, Acceptor>();
  List<PaxosPerKeyStore> peers;
```

Acceptor 存储了 promisedGeneration、acceptedGeneration 和 acceptedValue。

```java
class Acceptor…

  public class Acceptor {
      MonotonicId promisedGeneration = MonotonicId.empty();

      Optional<MonotonicId> acceptedGeneration = Optional.empty();
      Optional<Command> acceptedValue = Optional.empty();

      Optional<Command> committedValue = Optional.empty();
      Optional<MonotonicId> committedGeneration = Optional.empty();

      public AcceptorState state = AcceptorState.NEW;
      private BiConsumer<Acceptor, Command> kvStore;
```

当键值和值放到了 kv 存储时，它就运行了 Paxos 协议。

```java
class PaxosPerKeyStore…

  int maxKnownPaxosRoundId = 1;
  int maxAttempts = 4;
  public void put(String key, String defaultProposal) {
      int attempts = 0;
      while(attempts <= maxAttempts) {
          attempts++;
          MonotonicId requestId = new MonotonicId(maxKnownPaxosRoundId++, serverId);
          SetValueCommand setValueCommand = new SetValueCommand(key, defaultProposal);

          if (runPaxos(key, requestId, setValueCommand)) {
              return;
          }

          Uninterruptibles.sleepUninterruptibly(ThreadLocalRandom.current().nextInt(100), MILLISECONDS);
          logger.warn("Experienced Paxos contention. Attempting with higher generation");
      }
      throw new WriteTimeoutException(attempts);
  }

  private boolean runPaxos(String key, MonotonicId generation, Command initialValue) {
      List<Acceptor> allAcceptors = getAcceptorInstancesFor(key);
      List<PrepareResponse> prepareResponses = sendPrepare(generation, allAcceptors);
      if (isQuorumPrepared(prepareResponses)) {
          Command proposedValue = getValue(prepareResponses, initialValue);
          if (sendAccept(generation, proposedValue, allAcceptors)) {
              sendCommit(generation, proposedValue, allAcceptors);
          }
          if (proposedValue == initialValue) {
              return true;
          }
      }
      return false;
  }

  public Command getValue(List<PrepareResponse> prepareResponses, Command initialValue) {
      PrepareResponse mostRecentAcceptedValue = getMostRecentAcceptedValue(prepareResponses);
      Command proposedValue
              = mostRecentAcceptedValue.acceptedValue.isEmpty() ?
              initialValue : mostRecentAcceptedValue.acceptedValue.get();
      return proposedValue;
  }

  private PrepareResponse getMostRecentAcceptedValue(List<PrepareResponse> prepareResponses) {
      return prepareResponses.stream().max(Comparator.comparing(r -> r.acceptedGeneration.orElse(MonotonicId.empty()))).get();
  }
class Acceptor…

  public PrepareResponse prepare(MonotonicId generation) {

      if (promisedGeneration.isAfter(generation)) {
          return new PrepareResponse(false, acceptedValue, acceptedGeneration, committedGeneration, committedValue);
      }
      promisedGeneration = generation;
      state = AcceptorState.PROMISED;
      return new PrepareResponse(true, acceptedValue, acceptedGeneration, committedGeneration, committedValue);

  }
class Acceptor…

  public boolean accept(MonotonicId generation, Command value) {
      if (generation.equals(promisedGeneration) || generation.isAfter(promisedGeneration)) {
          this.promisedGeneration = generation;
          this.acceptedGeneration = Optional.of(generation);
          this.acceptedValue = Optional.of(value);
          return true;
      }
      state = AcceptorState.ACCEPTED;
      return false;
  }
```

只有当值成功地提交时，它才会存储到 kv 存储中。

```java
class Acceptor…

  public void commit(MonotonicId generation, Command value) {
      committedGeneration = Optional.of(generation);
      committedValue = Optional.of(value);
      state = AcceptorState.COMMITTED;
      kvStore.accept(this, value);
  }
class PaxosPerKeyStore…

  private void accept(Acceptor acceptor, Command command) {
      if (command instanceof SetValueCommand) {
          SetValueCommand setValueCommand = (SetValueCommand) command;
          kv.put(setValueCommand.getKey(), setValueCommand.getValue());
      }
      acceptor.resetPaxosState();
  }
```

Paxos 状态需要持久化。使用[预写日志（Write-Ahead Log）](write-ahead-log.md)可以轻松做到这一点。

#### 处理多值

值得注意的是，Paxos 在处理单值上有详细的做法，而且得到了证明。因此，用单值 Paxos 协议处理多值需要在协议规范之外进行处理。一种替代方法是重置状态，单独存储提交过的值，以确保它们不会丢失。

```java
class Acceptor…

  public void resetPaxosState() {
      //This implementation has issues if committed values are not stored
      //and handled separately in the prepare phase.
      //See Cassandra implementation for details.
      //https://github.com/apache/cassandra/blob/trunk/src/java/org/apache/cassandra/db/SystemKeyspace.java#L1232
      promisedGeneration = MonotonicId.empty();
      acceptedGeneration = Optional.empty();
      acceptedValue = Optional.empty();
  }
```

[(gryadka)[https://github.com/gryadka/js]]给出了另外一种做法，它稍微修改了一下基本的 Paxos 以便设置多个值。在基本的算法之外执行一些步骤，这种需求就是在实践中首选[复制日志（Replicated Log）](replicated-log.md)的原因。

#### 读取值

Paxos 依靠于准备阶段对任何未提交的值进行检测。因此，如果采用基本的 Paxos 实现如上所示的键值存储，那读取操作也需要运行完整的 Paxos 算法。

```java
class PaxosPerKeyStore…

  public String get(String key) {
      int attempts = 0;
      while(attempts <= maxAttempts) {
          attempts++;
          MonotonicId requestId = new MonotonicId(maxKnownPaxosRoundId++, serverId);
          Command getValueCommand = new NoOpCommand(key);
          if (runPaxos(key, requestId, getValueCommand)) {
              return kv.get(key);
          }

          Uninterruptibles.sleepUninterruptibly(ThreadLocalRandom.current().nextInt(100), MILLISECONDS);
          logger.warn("Experienced Paxos contention. Attempting with higher generation");

      }
      throw new WriteTimeoutException(attempts);
  }
```

## 示例

[cassandra](http://cassandra.apache.org/) 采用 Paxos 实现了轻量级事务。

所有的共识算法，比如 [Raft](https://raft.github.io/)，都采用了类似于基本的 Paxos 的基本概念。[两阶段提交（Two Phase Commit）](two-phase-commit.md)、[Quorum](quorum.md) 和[世代时钟（Generation Clock）](generation-clock.md)的使用方式都是类似的。
