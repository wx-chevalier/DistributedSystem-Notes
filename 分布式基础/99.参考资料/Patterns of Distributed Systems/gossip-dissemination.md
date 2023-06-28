# Gossip 传播（Gossip Dissemination）

**原文**

https://martinfowler.com/articles/patterns-of-distributed-systems/gossip-dissemination.html

使用节点的随机选择进行信息传递，以确保信息可以到达集群中的所有节点，而不会淹没网络。

**2021.6.17**

## 问题

在拥有多个节点的集群中，每个节点都要向集群中的所有其它节点传递其所拥有的元数据信息，无需依赖于共享存储。在一个很大的集群里，如果所有的服务器要和所有其它的服务器通信，就会消耗大量的网络带宽。信息应该能够到达所有节点，即便有些网络连接遇到一些问题。

在大型的集群中，需要考虑下面一些东西：

* 对每台服务器产生的信息数量进行固定的限制
* 消息不应消耗大量的网络带宽。应该有一个上限，比如说几百 Kbs，确保不会因为集群中有过多的消息影响到应用的数据传输。
* 元数据的传播应该可以容忍网络和部分服务器的失效。即便有一些网络链接中断，或是有部分服务器失效，消息也能到达所有的服务器节点。

正如边栏中所讨论的，Gossip 式的通信满足了所有这些要求。

每个集群节点都把元数据存储为一个键值对列表，每个键值都到关联集群的一个节点，就像下面这样：

```java
class Gossip…

  Map<NodeId, NodeState> clusterMetadata = new HashMap<>();
class NodeState…

  Map<String, VersionedValue> values = new HashMap<>();
```

启动时，每个集群节点都会添加关于自己的元数据，这些元数据需要传播给其他节点。元数据的一个例子可以是节点监听的 IP 地址和端口，它负责的分区等等。Gossip 实例需要知晓至少一个其它节点的情况，以便开始进行 Gossip 通信。有一个集群节点需要众所周知，用于初始化 Gossip 实例，这个节点称为种子节点（a seed node），或者创始节点（introducer）。任何节点都可以充当创始节点。

```java
class Gossip…

  public Gossip(InetAddressAndPort listenAddress,
                List<InetAddressAndPort> seedNodes,
                String nodeId) throws IOException {
      this.listenAddress = listenAddress;
      //filter this node itself in case its part of the seed nodes
      this.seedNodes = removeSelfAddress(seedNodes);
      this.nodeId = new NodeId(nodeId);
      addLocalState(GossipKeys.ADDRESS, listenAddress.toString());

      this.socketServer = new NIOSocketListener(newGossipRequestConsumer(), listenAddress);
  }

  private void addLocalState(String key, String value) {
      NodeState nodeState = clusterMetadata.get(listenAddress);
      if (nodeState == null) {
          nodeState = new NodeState();
          clusterMetadata.put(nodeId, nodeState);
      }
      nodeState.add(key, new VersionedValue(value, incremenetVersion()));
  }
```

每个集群节点都会调度一个 job 用以定期将其拥有的元数据传输给其他节点。

```java
class Gossip…

  private ScheduledThreadPoolExecutor gossipExecutor = new ScheduledThreadPoolExecutor(1);
  private long gossipIntervalMs = 1000;
  private ScheduledFuture<?> taskFuture;
  public void start() {
      socketServer.start();
      taskFuture = gossipExecutor.scheduleAtFixedRate(()-> doGossip(),
                  gossipIntervalMs,
                  gossipIntervalMs,
                  TimeUnit.MILLISECONDS);
  }
```

调用调度任务时，它会从元数据集合的服务器列表中随机选取一小群节点。我们会定义一个小的常数，称为 Gossip 扇出，它会确定会选取多少节点称为 Gossip 的目标。如果什么都不知道，它会随机选取一个种子节点，然后发送其拥有的元数据集合给该节点。

```java
class Gossip…

  public void doGossip() {
      List<InetAddressAndPort> knownClusterNodes = liveNodes();
      if (knownClusterNodes.isEmpty()) {
          sendGossip(seedNodes, gossipFanout);
      } else {
          sendGossip(knownClusterNodes, gossipFanout);
      }
  }

  private List<InetAddressAndPort> liveNodes() {
      Set<InetAddressAndPort> nodes
              = clusterMetadata.values()
              .stream()
              .map(n -> InetAddressAndPort.parse(n.get(GossipKeys.ADDRESS).getValue()))
              .collect(Collectors.toSet());
      return removeSelfAddress(nodes);
  }

private void sendGossip(List<InetAddressAndPort> knownClusterNodes, int gossipFanout) {
    if (knownClusterNodes.isEmpty()) {
        return;
    }

    for (int i = 0; i < gossipFanout; i++) {
        InetAddressAndPort nodeAddress = pickRandomNode(knownClusterNodes);
        sendGossipTo(nodeAddress);
    }
}

private void sendGossipTo(InetAddressAndPort nodeAddress) {
    try {
        getLogger().info("Sending gossip state to " + nodeAddress);
        SocketClient<RequestOrResponse> socketClient = new SocketClient(nodeAddress);
        GossipStateMessage gossipStateMessage
                = new GossipStateMessage(this.clusterMetadata);
        RequestOrResponse request
                = createGossipStateRequest(gossipStateMessage);
        byte[] responseBytes = socketClient.blockingSend(request);
        GossipStateMessage responseState = deserialize(responseBytes);
        merge(responseState.getNodeStates());

    } catch (IOException e) {
        getLogger().error("IO error while sending gossip state to " + nodeAddress, e);
    }
}

private RequestOrResponse createGossipStateRequest(GossipStateMessage gossipStateMessage) {
    return new RequestOrResponse(RequestId.PushPullGossipState.getId(),
            JsonSerDes.serialize(gossipStateMessage), correlationId++);
}
```

接收 Gossip 消息的集群节点会检查其拥有的元数据，发现三件事。

* 传入消息中的值，且不再该节点状态集合中
* 该节点拥有，但不再传入的 Gossip 消息中
* 节点拥有传入消息的值，这时会选择版本更高的值

稍后，它会将缺失的值添加到自己的状态集合中。传入消息中若有任何值缺失，就会在应答中返回这些值。

发送 Gossip 消息的集群节点会将从 Gossip 应答中得到值添加到自己的状态中。

```java
class Gossip…

  private void handleGossipRequest(org.distrib.patterns.common.Message<RequestOrResponse> request) {
      GossipStateMessage gossipStateMessage = deserialize(request.getRequest());
      Map<NodeId, NodeState> gossipedState = gossipStateMessage.getNodeStates();
      getLogger().info("Merging state from " + request.getClientSocket());
      merge(gossipedState);

      Map<NodeId, NodeState> diff = delta(this.clusterMetadata, gossipedState);
      GossipStateMessage diffResponse = new GossipStateMessage(diff);
      getLogger().info("Sending diff response " + diff);
      request.getClientSocket().write(new RequestOrResponse(RequestId.PushPullGossipState.getId(),
                      JsonSerDes.serialize(diffResponse),
                      request.getRequest().getCorrelationId()));
  }
public Map<NodeId, NodeState> delta(Map<NodeId, NodeState> fromMap, Map<NodeId, NodeState> toMap) {
    Map<NodeId, NodeState> delta = new HashMap<>();
    for (NodeId key : fromMap.keySet()) {
        if (!toMap.containsKey(key)) {
            delta.put(key, fromMap.get(key));
            continue;
        }
        NodeState fromStates = fromMap.get(key);
        NodeState toStates = toMap.get(key);
        NodeState diffStates = fromStates.diff(toStates);
        if (!diffStates.isEmpty()) {
            delta.put(key, diffStates);
        }
    }
    return delta;
}
public void merge(Map<NodeId, NodeState> otherState) {
    Map<NodeId, NodeState> diff = delta(otherState, this.clusterMetadata);
    for (NodeId diffKey : diff.keySet()) {
        if(!this.clusterMetadata.containsKey(diffKey)) {
            this.clusterMetadata.put(diffKey, diff.get(diffKey));
        } else {
            NodeState stateMap = this.clusterMetadata.get(diffKey);
            stateMap.putAll(diff.get(diffKey));
        }
    }
}
```

每隔一秒，这个过程就会在集群的每个节点上发生一次，每次都会选择不同的节点进行状态交换。

### 避免不必要的状态交换

上面的代码例子显示，在 Gossip 消息里发送了节点的完整状态。对于新加入的节点，这是没问题的，但一旦状态是最新的，就没有必要发送完整状态了。集群节点只需要发送自上个 Gossip 消息以来的状态变化。为了实现这一点，每个节点都维护着一个版本号，每当本地添加了一个新的元数据条目，这个版本就会递增一次。

```java
class Gossip…

  private int gossipStateVersion = 1;


  private int incremenetVersion() {
      return gossipStateVersion++;
  }
```

集群元数据的每个值都维护有一个版本号。这就是[有版本的值（Versioned Value）](versioned-value.md)这个模式的一个例子。

```java
class VersionedValue…

  int version;
  String value;

  public VersionedValue(String value, int version) {
      this.version = version;
      this.value = value;
  }

  public int getVersion() {
      return version;
  }

  public String getValue() {
      return value;
  }
```

之后，每个 Gossip 循环都可以交换从特定版本开始的状态。

```java
class Gossip…

  private void sendKnownVersions(InetAddressAndPort gossipTo) throws IOException {
      Map<NodeId, Integer> maxKnownNodeVersions = getMaxKnownNodeVersions();
      RequestOrResponse knownVersionRequest = new RequestOrResponse(RequestId.GossipVersions.getId(),
              JsonSerDes.serialize(new GossipStateVersions(maxKnownNodeVersions)), 0);
      SocketClient<RequestOrResponse> socketClient = new SocketClient(gossipTo);
      byte[] knownVersionResponseBytes = socketClient.blockingSend(knownVersionRequest);
  }

  private Map<NodeId, Integer> getMaxKnownNodeVersions() {
      return clusterMetadata.entrySet()
              .stream()
              .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().maxVersion()));
  }
class NodeState…

  public int maxVersion() {
      return values.values().stream().map(v -> v.getVersion()).max(Comparator.naturalOrder()).orElse(0);
  }
```

再之后，接收节点只会把版本号大于请求中版本号的那些值发送出去。

```java
class Gossip…

  Map<NodeId, NodeState> getMissingAndNodeStatesHigherThan(Map<NodeId, Integer> nodeMaxVersions) {
      Map<NodeId, NodeState> delta = new HashMap<>();
      delta.putAll(higherVersionedNodeStates(nodeMaxVersions));
      delta.putAll(missingNodeStates(nodeMaxVersions));
      return delta;
  }

  private Map<NodeId, NodeState> missingNodeStates(Map<NodeId, Integer> nodeMaxVersions) {
      Map<NodeId, NodeState> delta = new HashMap<>();
      List<NodeId> missingKeys = clusterMetadata.keySet().stream().filter(key -> !nodeMaxVersions.containsKey(key)).collect(Collectors.toList());
      for (NodeId missingKey : missingKeys) {
          delta.put(missingKey, clusterMetadata.get(missingKey));
      }
      return delta;
  }

  private Map<NodeId, NodeState> higherVersionedNodeStates(Map<NodeId, Integer> nodeMaxVersions) {
      Map<NodeId, NodeState> delta = new HashMap<>();
      Set<NodeId> keySet = nodeMaxVersions.keySet();
      for (NodeId node : keySet) {
          Integer maxVersion = nodeMaxVersions.get(node);
          NodeState nodeState = clusterMetadata.get(node);
          if (nodeState == null) {
              continue;
          }
          NodeState deltaState = nodeState.statesGreaterThan(maxVersion);
          if (!deltaState.isEmpty()) {
              delta.put(node, deltaState);
          }
      }
      return delta;
  }
```

[cassandra](http://cassandra.apache.org/) 的 Gossip 实现通过三次握手优化了状态交换，接收 Gossip 消息的节点也会发出它在发送者那里所需的版本，以及它返回的元数据。然后，发送者立即在应答中给出了请求的元数据。这样就避免原本需要的额外消息。

[cockroachdb](https://www.cockroachlabs.com/docs/stable/) 使用的 Gossip 协议维护每个相连节点的状态。对每个连接来说，它都维护着发送给那个节点最后的版本，以及从那个节点接收到的版本。这是为了让它能够发送“从最后发送的版本以来的值”，以及请求“从最后收到版本开始的状态”。

还可以使用其它的一些高效的替代方案，比如，发送整个状态集的哈希值，如果哈希值相同，则什么都不做。

### Gossip 节点选择的标准

集群节点可以随机选择节点发送 Gossip 消息。下面是一个用 Java 实现的例子，使用了 java.util.Random：

```java
class Gossip…

  private Random random = new Random();
  private InetAddressAndPort pickRandomNode(List<InetAddressAndPort> knownClusterNodes) {
      int randomNodeIndex = random.nextInt(knownClusterNodes.size());
      InetAddressAndPort gossipTo = knownClusterNodes.get(randomNodeIndex);
      return gossipTo;
  }
```

还可以有其它的考量，比如，选择之前联系最少的节点。比如，[Cockroachdb](https://github.com/cockroachdb/cockroach/blob/master/docs/design.md) 的 Gossip 协议就是这么选择节点的。

还存在一些[感知网络拓扑（network-topology-aware）](https://dl.acm.org/doi/10.1109/TPDS.2006.85)的 Gossip 目标选择的方式。

所有这些方法都可以以模块化的方式实现在 pickRandomNode() 方法里。

### 分组成员和失效检测

维护集群中的可用节点列表是 Gossip 协议最常见的用法之一。有两种方式在使用。

* [swim-gossip](https://www.cs.cornell.edu/projects/Quicksilver/public_pdfs/SWIM.pdf) 使用了一个单独的探测组件，它会不断地探测集群中的不同节点，以检测它们是否可用。如果它检测到某个节点是活的或死的，这个结果会通过 Gossip 通信传播给整个集群。探测器会随机选择一个节点发送 Gossip 消息。如果接收节点检测这是一条新的消息，它会立即将消息发送给一个随机选择的节点。这样，如果集群中有节点失效或者有新加入的节点，整个集群很快就都知道了。

* 集群节点可以定期更新自己的状态以反映其心跳，稍后，这种状态通过 Gossip 消息的交换传播到整个集群。然后，每个集群节点都可以检查它是否在固定的时间内收到某个特定集群节点的更新，否则，就将该节点标记为宕机。在这种情况下，每个集群节点独立地确定一个节点是在运行中还是宕机了。

### 处理节点重启

在节点崩溃或重启的情况下，有版本的值就不能很好的运作了，因为所有的内存状态都会丢失。更重要的是，对于同样的键值，节点可能会有不同的值。比如，集群节点以不同的 IP 地址和端口启动，或是以不同配置启动。[世代时钟（Generation Clock）](generation-clock.md)可以用来标记每个值的世代，因此，当元数据状态发送给一个随机的集群节点，接收的节点就不仅可以凭借版本号，还可以用世代信息检测变化。

值得注意的是，对于 Gossip 协议的工作而言，这个机制并非必要的。但在实践中，这个实现能够确保状态变化得到正确地跟踪。

## 示例

[cassandra](http://cassandra.apache.org/) 使用 Gossip 协议处理集群节点的分组成员和失效检测。每个集群节点的元数据，诸如分配给每个集群节点的令牌，也使用 Gossip 协议进行传输。

[consul](https://www.consul.io/) 使用 [swim-gossip](https://www.cs.cornell.edu/projects/Quicksilver/public_pdfs/SWIM.pdf) 协议处理 consul 代理的分组成员和失效检测。

[cockroachdb](https://www.cockroachlabs.com/docs/stable/) 使用 Gossip 协议传播节点的元数据。

像 [Hyperledger Fabric](https://hyperledger-fabric.readthedocs.io/en/release-2.2/gossip.html) 这样的区块链实现会使用 Gossip 协议处理分组成员以及发送账本的元数据。