# 算法对比

分布式一致性，简单的说就是在一个或多个进程提议了一个值后，使系统中所有进程对这个值达成一致。

![简要的一致性描述](https://s1.ax1x.com/2020/08/01/aGlZtK.jpg)

为了就某个值达成一致，每个进程都可以提出自己的提议，最终通过分布式一致性算法，所有正确运行的进程学习到相同的值。工业界对分布式一致性的应用，都是为了构建多副本状态机模型（Replicated State Machine），实现高可用和强一致。

![多副本状态机](https://s1.ax1x.com/2020/08/01/aGGTqH.png)

分布式一致性使多台机器具有相同的状态，运行相同的确定性状态机，在少数机器故障时整体仍能正常工作。

![状态机流程](https://s1.ax1x.com/2020/08/01/aGJtyD.png)

# Paxos

Paxos 达成一个决议至少需要两个阶段（Prepare 阶段和 Accept 阶段）。

![Paxos 多阶段描述](https://s1.ax1x.com/2020/08/02/atMCSU.jpg)

- Prepare 阶段的作用：争取提议权，争取到了提议权才能在 Accept 阶段发起提议，否则需要重新争取；学习之前已经提议的值。
- Accept 阶段使提议形成多数派，提议一旦形成多数派则决议达成，可以开始学习达成的决议。Accept 阶段若被拒绝需要重新走 Prepare 阶段。

# Multi-Paxos

Basic Paxos 达成一次决议至少需要两次网络来回，并发情况下可能需要更多，极端情况下甚至可能形成活锁，效率低下，Multi-Paxos 正是为解决此问题而提出。

![Multi-Paxos](https://s1.ax1x.com/2020/08/03/aduEHH.md.png)

Multi-Paxos 选举一个 Leader，提议由 Leader 发起，没有竞争，解决了活锁问题。提议都由 Leader 发起的情况下，Prepare 阶段可以跳过，将两阶段变为一阶段，提高效率。Multi-Paxos 并不假设唯一 Leader，它允许多 Leader 并发提议，不影响安全性，极端情况下退化为 Basic Paxos。

Multi-Paxos 与 Basic Paxos 的区别并不在于 Multi（Basic Paxos 也可以 Multi），只是在同一 Proposer 连续提议时可以优化跳过 Prepare 直接进入 Accept 阶段，仅此而已。

# Raft

不同于 Paxos 直接从分布式一致性问题出发推导出来，Raft 则是从多副本状态机的角度提出，使用更强的假设来减少需要考虑的状态，使之变的易于理解和实现。

# EPaxos

EPaxos（Egalitarian Paxos）于 SOSP'13 提出，比 Raft 还稍早一些，但 Raft 在工业界大行其道的时间里，EPaxos 却长期无人问津，直到最近，EPaxos 开始被工业界所关注。EPaxos 是一个 Leaderless 的一致性算法，任意副本均可提交日志，通常情况下，一次日志提交需要一次或两次网络来回。EPaxos 无 Leader 选举开销，一个副本不可用可立即访问其他副本，具有更高的可用性。各副本负载均衡，无 Leader 瓶颈，具有更高的吞吐量。客户端可选择最近的副本提供服务，在跨 AZ 跨地域场景下具有更小的延迟。

不同于 Paxos 和 Raft，事先对所有 Instance 编号排序，然后再对每个 Instance 的值达成一致。EPaxos 不事先规定 Instance 的顺序，而是在运行时动态决定各 Instance 之间的顺序。EPaxos 不仅对每个 Instance 的值达成一致，还对 Instance 之间的相对顺序达成一致。EPaxos 将不同 Instance 之间的相对顺序也做为一致性问题，在各个副本之间达成一致，因此各个副本可并发地在各自的 Instance 中发起提议，在这些 Instance 的值和相对顺序达成一致后，再对它们按照相对顺序重新排序，最后按顺序应用到状态机。

从图论的角度看，日志是图的结点，日志之间的顺序是图的边，EPaxos 对结点和边分别达成一致，然后使用拓扑排序，决定日志的顺序。图中也可能形成环路，EPaxos 需要处理循环依赖的问题。EPaxos 引入日志冲突的概念（与 Parallel Raft 类似，与并发冲突不是一个概念），若两条日志之间没有冲突（例如访问不同的 key），则它们的相对顺序无关紧要，因此 EPaxos 只处理有冲突的日志之间的相对顺序。

若并发提议的日志之间没有冲突，EPaxos 只需要运行 PreAccept 阶段即可提交（Fast Path），否则需要运行 Accept 阶段才能提交（Slow Path）。

![PreAccept](https://s1.ax1x.com/2020/08/03/adKkR0.png)

PreAccept 阶段尝试将日志以及与其它日志之间的相对顺序达成一致，同时维护该日志与其它日志之间的冲突关系，如果运行完 PreAccept 阶段，没有发现该日志与其它并发提议的日志之间有冲突，则该日志以及与其它日志之间的相对顺序已经达成一致，直接发送异步的 Commit 消息提交；否则如果发现该日志与其它并发提议的日志之间有冲突，则日志之间的相对顺序还未达成一致，需要运行 Accept 阶段将冲突依赖关系达成多数派，再发送 Commit 消息提交。

![PreAccept](https://s1.ax1x.com/2020/08/03/adKeLF.png)

EPaxos 的 Fast Path Quorum 为 2F，可优化至 `F + [ (F + 1) / 2 ]`，在 3 副本和 5 副本时，与 Paxos、Raft 一致。Slow Path 为 Paxos Accept 阶段，Quorum 固定为 F + 1。EPaxos 还有一个主动 Learn 的算法，在恢复的时候可用来追赶日志，这里就不做具体的介绍了，感兴趣的可以看论文。

# 算法对比

## 可理解性

众所周知，Paxos 是出了名的晦涩难懂，不仅难以理解，更难以实现。而 Raft 则以可理解性和易于实现为目标，Raft 的提出大大降低了使用分布式一致性的门槛，将分布式一致性变的大众化、平民化，因此当 Raft 提出之后，迅速得到青睐，极大地推动了分布式一致性的工程应用。

EPaxos 的提出比 Raft 还早，但却长期无人问津，很大一个原因就是 EPaxos 实在是难以理解。EPaxos 基于 Paxos，但却比 Paxos 更难以理解，大大地阻碍了 EPaxos 的工程应用。不过，是金子总会发光的，EPaxos 因着它独特的优势，终于被人们发现，具有广阔的前景。

## 效率

从 Paxos 到 Raft 再到 EPaxos，效率有没有提升呢？我们主要从负载均衡、消息复杂度、Pipeline 以及并发处理几个方面来对比 Multi-Paxos、Raft 和 EPaxos。

### 负载均衡

Multi-Paxos 和 Raft 的 Leader 负载更高，各副本之间负载不均衡，Leader 容易成为瓶颈，而 EPaxos 无需 Leader，各副本之间负载完全均衡。

### 消息复杂度

Multi-Paxos 和 Raft 选举出 Leader 之后，正常只需要一次网络来回就可以提交一条日志，但 Multi-Paxos 需要额外的异步 Commit 消息提交，Raft 只需要推进本地的 commit index，不使用额外的消息，EPaxos 根据日志冲突情况需要一次或两次网络来回。因此消息复杂度，Raft 最低，Paxos 其次，EPaxos 最高。

### Pipeline

我们将 Pipeline 分为顺序 Pipeline 和乱序 Pipeline。Multi-Paxos 和 EPaxos 支持乱序 Pipeline，Raft 因为日志连续性假设，只支持顺序 Pipeline。但 Raft 也可以实现乱序 Pipeline，只需要在 Leader 上给每个 Follower 维护一个类似于 TCP 的滑动窗口，对应每个 Follower 上维护一个接收窗口，允许窗口里面的日志不连续，窗口外面是已经连续的日志，日志一旦连续则向前滑动窗口，窗口里面可乱序 Pipeline。

### 并发处理

Multi-Paxos 沿用 Paxos 的策略，一旦发现并发冲突则回退重试，直到成功；Raft 则使用强 Leader 来避免并发冲突，Follwer 不与 Leader 竞争，避免了并发冲突；EPaxos 则直面并发冲突问题，将冲突依赖也做为一致性问题对待，解决并发冲突。Paxos 是冲突回退，Raft 是冲突避免，EPaxos 是冲突解决。Paxos 和 Raft 的日志都是线性的，而 EPaxos 的日志是图状的，因此 EPaxos 的并行性更好，吞吐量也更高。

## 可用性

EPaxos 任意副本均可提供服务，某个副本不可用了可立即切换到其它副本，副本失效对可用性的影响微乎其微；而 Multi-Paxos 和 Raft 均依赖 Leader，Leader 不可用了需要重新选举 Leader，在新 Leader 未选举出来之前服务不可用。显然 EPaxos 的可用性比 Multi-Paxos 和 Raft 更好，但 Multi-Paxos 和 Raft 比谁的可用性更好呢。

Raft 是强 Leader，Follower 必须等旧 Leader 的 Lease 到期后才能发起选举，Multi-Paxos 是弱 Leader，Follwer 可以随时竞选 Leader，虽然会对效率造成一定影响，但在 Leader 失效的时候能更快的恢复服务，因此 Multi-Paxos 比 Raft 可用性更好。

## 适用场景

EPaxos 更适用于跨 AZ 跨地域场景，对可用性要求极高的场景，Leader 容易形成瓶颈的场景。Multi-Paxos 和 Raft 本身非常相似，适用场景也类似，适用于内网场景，一般的高可用场景，Leader 不容易形成瓶颈的场景。
