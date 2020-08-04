# Raft

熟悉或了解分布性系统的开发者都知道一致性算法的重要性，Paxos 一致性算法从 90 年提出到现在已经有二十几年了，而 Paxos 流程太过于繁杂实现起来也比较复杂，可能也是以为过于复杂 现在我听说过比较出名使用到 Paxos 的也就只是 Chubby、libpaxos，搜了下发现 Keyspace、BerkeleyDB 数据库中也使用了该算法作为数据的一致性同步，虽然现在很广泛使用的 Zookeeper 也是基于 Paxos 算法来实现，但是 Zookeeper 使用的 ZAB（Zookeeper Atomic Broadcast）协议对 Paxos 进行了很多的改进与优化，算法复杂我想会是制约他发展的一个重要原因；说了这么多只是为了要引出本篇文章的主角 Raft 一致性算法，没错 Raft 就是在这个背景下诞生的，文章开头也说到了 Paxos 最大的问题就是复杂，Raft 一致性算法就是比 Paxos 简单又能实现 Paxos 所解决的问题的一致性算法。

Raft 是斯坦福的 Diego Ongaro、John Ousterhout 两个人以易懂（Understandability）为目标设计的一致性算法，在 2013 年发布了论文：《In Search of an Understandable Consensus Algorithm》从 2013 年发布到现在不过只有两年，到现在已经有了十多种语言的 Raft 算法实现框架，较为出名的有 etcd，Google 的 Kubernetes 也是用了 etcd 作为他的服务发现框架；由此可见易懂性是多么的重要。

# Raft 概述

Raft 是一个用于日志复制，同步的一致性算法。它提供了和 Paxos 一样的功能和性能，但是它的算法结构与 Paxos 不同。这使得 Raft 相比 Paxos 更好理解，并且更容易构建实际的系统。为了强调可理解性，Raft 将一致性算法分解为几个关键流程（模块），例如选主，安全性，日志复制，通过将分布式一致性这个复杂的问题转化为一系列的小问题进而各个击破的方式来解决问题。同时它通过实施一个更强的一致性来减少一些不必要的状态，进一步降低了复杂性。Raft 还包括了一个新机制，允许线上进行动态的集群扩容，利用有交集的大多数机制来保证安全性。

与 Paxos 不同 Raft 强调的是易懂（Understandability），Raft 和 Paxos 一样只要保证 `n/2+1` 节点正常就能够提供服务；众所周知但问题较为复杂时可以把问题分解为几个小问题来处理，Raft 也使用了分而治之的思想把算法流程分为三个子问题：选举（Leader election）、日志复制（Log replication）、安全性（Safety）三个子问题；这里先简单介绍下 Raft 的流程：

- Raft 开始时在集群中选举出 Leader 负责日志复制的管理；
- Leader 接受来自客户端的事务请求（日志），并将它们复制给集群的其他节点，然后负责通知集群中其他节点提交日志，Leader 负责保证其他节点与他的日志同步；
- 当 Leader 宕掉后集群其他节点会发起选举选出新的 Leader；
