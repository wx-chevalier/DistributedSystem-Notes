# Raft

熟悉或了解分布性系统的开发者都知道一致性算法的重要性，Paxos 一致性算法从 90 年提出到现在已经有二十几年了，而 Paxos 流程太过于繁杂实现起来也比较复杂，可能也是以为过于复杂 现在我听说过比较出名使用到 Paxos 的也就只是 Chubby、libpaxos，搜了下发现 Keyspace、BerkeleyDB 数据库中也使用了该算法作为数据的一致性同步，虽然现在很广泛使用的 Zookeeper 也是基于 Paxos 算法来实现，但是 Zookeeper 使用的 ZAB（Zookeeper Atomic Broadcast）协议对 Paxos 进行了很多的改进与优化，算法复杂我想会是制约他发展的一个重要原因；说了这么多只是为了要引出本篇文章的主角 Raft 一致性算法，没错 Raft 就是在这个背景下诞生的，文章开头也说到了 Paxos 最大的问题就是复杂，Raft 一致性算法就是比 Paxos 简单又能实现 Paxos 所解决的问题的一致性算法。

Raft 是斯坦福的 Diego Ongaro、John Ousterhout 两个人以易懂（Understandability）为目标设计的一致性算法，在 2013 年发布了论文：《In Search of an Understandable Consensus Algorithm》从 2013 年发布到现在不过只有两年，到现在已经有了十多种语言的 Raft 算法实现框架，较为出名的有 etcd，Google 的 Kubernetes 也是用了 etcd 作为他的服务发现框架；由此可见易懂性是多么的重要。

# Raft 与 Multi-Paxos 对比

Raft 与 Multi-Paxos 有着千丝万缕的关系，下面总结了 Raft 与 Multi-Paxos 的异同。Raft 与 Multi-Paxos 中相似的概念：

![Raft 与 Multi-Paxos 对比](https://s1.ax1x.com/2020/08/03/adut5n.png)

- Raft 的 Leader 即 Multi-Paxos 的 Proposer。
- Raft 的 Term 与 Multi-Paxos 的 Proposal ID 本质上是同一个东西。
- Raft 的 Log Entry 即 Multi-Paxos 的 Proposal。
- Raft 的 Log Index 即 Multi-Paxos 的 Instance ID。
- Raft 的 Leader 选举跟 Multi-Paxos 的 Prepare 阶段本质上是相同的。
- Raft 的日志复制即 Multi-Paxos 的 Accept 阶段。

Raft 与 Multi-Paxos 的不同：

![Raft 与 Multi-Paxos 的不同](https://s1.ax1x.com/2020/08/03/adufxK.png)

Raft 假设系统在任意时刻最多只有一个 Leader，提议只能由 Leader 发出（强 Leader），否则会影响正确性；而 Multi-Paxos 虽然也选举 Leader，但只是为了提高效率，并不限制提议只能由 Leader 发出（弱 Leader）。强 Leader 在工程中一般使用 Leader Lease 和 Leader Stickiness 来保证：

- Leader Lease：上一任 Leader 的 Lease 过期后，随机等待一段时间再发起 Leader 选举，保证新旧 Leader 的 Lease 不重叠。
- Leader Stickiness：Leader Lease 未过期的 Follower 拒绝新的 Leader 选举请求。

Raft 限制具有最新已提交的日志的节点才有资格成为 Leader，Multi-Paxos 无此限制。Raft 在确认一条日志之前会检查日志连续性，若检查到日志不连续会拒绝此日志，保证日志连续性，Multi-Paxos 不做此检查，允许日志中有空洞。Raft 在 AppendEntries 中携带 Leader 的 commit index，一旦日志形成多数派，Leader 更新本地的 commit index 即完成提交，下一条 AppendEntries 会携带新的 commit index 通知其它节点；Multi-Paxos 没有日志连接性假设，需要额外的 commit 消息通知其它节点。

# Links

- 建议在 [The Secret Lives of Data](http://thesecretlivesofdata.com/) 查看 Raft 算法的动画演示讲解。
