# Paxos

> Google Chubby 的作者 Mike Burrows 说过这个世界上只有一种一致性算法，那就是 Paxos，其它的算法都是残次品。

```
        --- Paxos Proposer ---

     1	proposer(v):
     2    while not decided:
     2	    choose n, unique and higher than any n seen so far
     3	    send prepare(n) to all servers including self
     4	    if prepare_ok(n, na, va) from majority:
     5	      v' = va with highest na; choose own v otherwise
     6	      send accept(n, v') to all
     7	      if accept_ok(n) from majority:
     8	        send decided(v') to all


        --- Paxos Acceptor ---

     9	acceptor state on each node (persistent):
    10	 np     --- highest prepare seen
    11	 na, va --- highest accept seen

    12	acceptor's prepare(n) handler:
    13	 if n > np
    14	   np = n
    15	   reply prepare_ok(n, na, va)
    16   else
    17     reply prepare_reject


    18	acceptor's accept(n, v) handler:
    19	 if n >= np
    20	   np = n
    21	   na = n
    22	   va = v
    23	   reply accept_ok(n)
    24   else
    25     reply accept_reject
```

PAXOS 可以用来解决分布式环境下，选举(或设置)某一个值的问题(比如更新数据库中某个 user 的 age 是多少)。分布式系统中有多个节点就会存在节点间通信的问题，存在着两种节点通讯模型：共享内存(Shared memory)、消息传递(Messages passing)，Paxos 是基于消息传递的通讯模型的。它的假设前提是，在分布式系统中进程之间的通信会出现丢失、延迟、重复等现象，但不会出现传错的现象。Paxos 算法就是为了保证在这样的系统中进程间基于消息传递就某个值达成一致。

> 注意，在 Paxos 算法中是不考虑拜占庭将军问题的

## Scenario:场景介绍

比如下面的情况：有三个服务器进程 A,B,C 运行在三台主机上提供数据库服务，当一个 client 连接到任何一台服务器上的时候，都能通过该服务器进行 read 和 update 的操作。首先先看一个 A、B、C 不那么均等的方法：
A、B、C 选举主机名最大的服务器为 master 提供服务，所有的 read、update 操作都发生在它上面，其余的两台服务器只是 slave(后者，在这里类似 proxy)，当 client 是连接在 master 的服务器的时候，其直接和 master 交互，就类似于单机的场景。当 client 时连接在 slave 的时候，所有的请求被转移到了 master 上进行操作，然后再结果返回给 client。如果 master 挂了，那么剩余两台主机在检测到 master 挂的情况后，再根据主机名最大的方法选举 master。上面的算法有一个问题，它让 A、B、C 不那么均等了，A、B、C 存在角色之分，且在某个时候 master 挂机后，需要立刻选举出新的 master 提供服务。同时，这个算法还要求各个服务器之间保持心跳。而 PAXOS 算法则不同，PAXOS 提供了一种将 A、B、C 等价的方式提供上面的操作，并保证数据的正确性。在某台主机宕机后，只要总数一半以上的服务器还存活，则整个集群依然能对外提供服务，甚至不需要心跳。

# 算法原理

## Terminology:名词解释

为描述 Paxos 算法，Lamport 虚拟了一个叫做 Paxos 的[希腊城邦](https://zh.wikipedia.org/wiki/%E5%B8%8C%E8%87%98%E5%9F%8E%E9%82%A6)，这个岛按照议会民主制的政治模式制订法律，但是没有人愿意将自己的全部时间和精力放在这种事情上。所以无论是议员，议长或者传递纸条的服务员都不能承诺别人需要时一定会出现，也无法承诺批准决议或者传递消息的时间。但是这里假设没有[拜占庭将军问题](https://zh.wikipedia.org/wiki/%E6%8B%9C%E5%8D%A0%E5%BA%AD%E5%B0%86%E5%86%9B%E9%97%AE%E9%A2%98)(Byzantine failure，即虽然有可能一个消息被传递了两次，但是绝对不会出现错误的消息)；只要等待足够的时间，消息就会被传到。另外，Paxos 岛上的议员是不会反对其他议员提出的决议的。首先将议员的角色分为 proposers，Acceptors，和 learners(允许身兼数职)。proposers 提出提案，提案信息包括提案编号和提议的 value；Acceptor 收到提案后可以接受(accept)提案，若提案获得多数 Acceptors 的接受，则称该提案被批准(chosen)；learners 只能“学习”被批准的提案。划分角色后，就可以更精确的定义问题：

1. 决议(value)只有在被 proposers 提出后才能被批准(未经批准的决议称为“提案(proposal)”)；
2. 在一次 Paxos 算法的执行实例中，只批准(chosen)一个 value；
3. learners 只能获得被批准(chosen)的 value。
   ## Process:处理流程
   批准 value 的过程中，首先 proposers 将 value 发送给 Acceptors，之后 Acceptors 对 value 进行接受(accept)。为了满足只批准一个 value 的约束，要求经“多数派(majority)”接受的 value 成为正式的决议(称为“批准”决议)。这是因为无论是按照人数还是按照权重划分，两组“多数派”至少有一个公共的 Acceptor，如果每个 Acceptor 只能接受一个 value，约束 2 就能保证。整个过程(一个实例或称一个事务或一个 Round)分为两个阶段：

- phase1(准备阶段)
  - Proposer 向超过半数(n/2+1)Acceptor 发起 prepare 消息(发送编号)
  - 如果 prepare 符合协议规则 Acceptor 回复 promise 消息，否则拒绝
- phase2(决议阶段或投票阶段)
  - 如果超过半数 Acceptor 回复 promise，Proposer 向 Acceptor 发送 accept 消息(此时包含真实的值)
  - Acceptor 检查 accept 消息是否符合规则，消息符合则批准 accept 请求

根据上述过程当一个 proposer 发现存在编号更大的提案时将终止提案。这意味着提出一个编号更大的提案会终止之前的提案过程。如果两个 proposer 在这种情况下都转而提出一个编号更大的提案，就可能陷入活锁，违背了 Progress 的要求。这种情况下的解决方案是选举出一个 leader，仅允许 leader 提出提案。但是由于消息传递的不确定性，可能有多个 proposer 自认为自己已经成为 leader。Lamport 在 The Part-Time Parliament 一文中描述并解决了这个问题。

### 约束条件

##### P1: Acceptor 必须接受他接收到的第一个提案，

注意 P1 是不完备的。如果恰好一半 Acceptor 接受的提案具有 value A，另一半接受的提案具有 value B，那么就无法形成多数派，无法批准任何一个 value。

##### P2：当且仅当 Acceptor 没有回应过编号大于 n 的 prepare 请求时，Acceptor 接受(accept)编号为 n 的提案。

如果一个没有 chosen 过任何 proposer 提案的 Acceptor 在 prepare 过程中回答了一个 proposer 针对提案 n 的问题，但是在开始对 n 进行投票前，又接受(accept)了编号小于 n 的另一个提案(例如 n-1)，如果 n-1 和 n 具有不同的 value，这个投票就会违背 P2c。因此在 prepare 过程中，Acceptor 进行的回答同时也应包含承诺：不会再接受(accept)编号小于 n 的提案。

##### P3：只有 Acceptor 没有接受过提案 Proposer 才能采用自己的 Value，否者 Proposer 的 Value 提案为 Acceptor 中编号最大的 Proposer Value；

##### P4: 一个提案被选中需要过半数的 Acceptor 接受。

假设 A 为整个 Acceptor 集合，B 为一个超过 A 一半的 Acceptor 集合，B 为 A 的子集，C 也是一个超过 A 一半的 Acceptor 集合，C 也是 A 的子集，有此可知任意两个过半集合中必定有一个共同的成员 Acceptor；此说明了一个 Acceptor 可以接受不止一个提案，此时需要一个编号来标识每一个提案，提案的格式为：[编号，Value]，编号为不可重复全序的，因为存在着一个一个 Paxos 过程只能批准一个 value 这时又推出了一个约束 P3；

##### P5：当编号 K0、Value 为 V0 的提案(即[K0,V0])被过半的 Acceptor 接受后，今后(同一个 Paxos 或称一个 Round 中)所有比 K0 更高编号且被 Acceptor 接受的提案，其 Value 值也必须为 V0。

该约束还可以表述为：

- 一旦一个具有 value v 的提案被批准(chosen)，那么之后任何 Acceptor 再次接受(accept)的提案必须具有 value v。
- 一旦一个具有 value v 的提案被批准(chosen)，那么以后任何 proposer 提出的提案必须具有 value v。
- 如果一个编号为 n 的提案具有 value v，那么存在一个多数派，要么他们中所有人都没有接受(accept)编号小于 n
  的任何提案，要么他们已经接受(accept)的所有编号小于 n 的提案中编号最大的那个提案具有 value v。

因为每个 Proposer 都可提出多个议案，每个议案最初都有一个不同的 Value 所以要满足 P3 就又要推出一个新的约束 P4；

### 决议的发布

一个显而易见的方法是当 acceptors 批准一个 value 时，将这个消息发送给所有 learner。但是这个方法会导致消息量过大。

由于假设没有 Byzantine failures，learners 可以通过别的 learners 获取已经通过的决议。因此 acceptors 只需将批准的消息发送给指定的某一个 learner，其他 learners 向它询问已经通过的决议。这个方法降低了消息量，但是指定 learner 失效将引起系统失效。

因此 acceptors 需要将 accept 消息发送给 learners 的一个子集，然后由这些 learners 去通知所有 learners。

但是由于消息传递的不确定性，可能会没有任何 learner 获得了决议批准的消息。当 learners 需要了解决议通过情况时，可以让一个 proposer 重新进行一次提案。注意一个 learner 可能兼任 proposer。

### 议员税率问题实例

有 A1, A2, A3, A4, A5 5 位议员，就税率问题进行决议。议员 A1 决定将税率定为 10%,因此它向所有人发出一个草案。这个草案的内容是：

```
现有的税率是什么?如果没有决定，则建议将其定为10%.时间：本届议会第3年3月15日;提案者：A1
```

在最简单的情况下，没有人与其竞争;信息能及时顺利地传达到其它议员处。

于是, A2-A5 回应：

```
我已收到你的提案，等待最终批准
```

而 A1 在收到 2 份回复后就发布最终决议：

```
税率已定为10%,新的提案不得再讨论本问题。
```

这实际上退化为[二阶段提交](https://zh.wikipedia.org/wiki/%E4%BA%8C%E9%98%B6%E6%AE%B5%E6%8F%90%E4%BA%A4)协议。

现在我们假设在 A1 提出提案的同时, A5 决定将税率定为 20%:

```
现有的税率是什么?如果没有决定，则建议将其定为20%.时间：本届议会第3年3月15日;提案者：A5
```

草案要通过侍从送到其它议员的案头. A1 的草案将由 4 位侍从送到 A2-A5 那里。现在，负责 A2 和 A3 的侍从将草案顺利送达，负责 A4 和 A5 的侍从则不上班. A5 的草案则顺利的送至 A3 和 A4 手中。

现在, A1, A2, A3 收到了 A1 的提案; A3, A4, A5 收到了 A5 的提案。按照协议, A1, A2, A4, A5 将接受他们收到的提案，侍从将拿着

```
我已收到你的提案，等待最终批准
```

的回复回到提案者那里。

而 A3 的行为将决定批准哪一个。

#### 情况一:仅有单议员提出议案

假设 A1 的提案先送到 A3 处，而 A5 的侍从决定放假一段时间。于是 A3 接受并派出了侍从. A1 等到了两位侍从，加上它自己已经构成一个多数派，于是税率 10%将成为决议. A1 派出侍从将决议送到所有议员处：

```
税率已定为10%,新的提案不得再讨论本问题。
```

A3 在很久以后收到了来自 A5 的提案。由于税率问题已经讨论完毕，他决定不再理会。但是他要抱怨一句：

```
税率已在之前的投票中定为10%,你不要再来烦我!
```

这个回复对 A5 可能有帮助，因为 A5 可能因为某种原因很久无法与与外界联系了。当然更可能对 A5 没有任何作用，因为 A5 可能已经从 A1 处获得了刚才的决议。

#### 情况二:多议员提出议案，但是第一份议案在第二份议案广播前形成决议

依然假设 A1 的提案先送到 A3 处，但是这次 A5 的侍从不是放假了，只是中途耽搁了一会。这次, A3 依然会将"接受"回复给 A1.但是在决议成型之前它又收到了 A5 的提案。这时协议有两种处理方式：

1.如果 A5 的提案更早，按照传统应该由较早的提案者主持投票。现在看来两份提案的时间一样(本届议会第 3 年 3 月 15 日)。但是 A5 是个惹不起的大人物。于是 A3 回复：

```
我已收到您的提案，等待最终批准，但是您之前有人提出将税率定为10%,请明察。
```

于是, A1 和 A5 都收到了足够的回复。这时关于税率问题就有两个提案在同时进行。但是 A5 知道之前有人提出税率为 10%.于是 A1 和 A5 都会向全体议员广播：

```
 税率已定为10%,新的提案不得再讨论本问题。
```

一致性得到了保证。

\2. A5 是个无足轻重的小人物。这时 A3 不再理会他, A1 不久后就会广播税率定为 10%.

#### 情况三:多议员同时提出议案

在这个情况中，我们将看见，根据提案的时间及提案者的权势决定是否应答是有意义的。在这里，时间和提案者的权势就构成了给提案编号的依据。这样的编号符合"任何两个提案之间构成偏序"的要求。

A1 和 A5 同样提出上述提案，这时 A1 可以正常联系 A2 和 A3; A5 也可以正常联系这两个人。这次 A2 先收到 A1 的提案; A3 则先收到 A5 的提案. A5 更有权势。

在这种情况下，已经回答 A1 的 A2 发现有比 A1 更有权势的 A5 提出了税率 20%的新提案，于是回复 A5 说：

```
我已收到您的提案，等待最终批准。
```

而回复了 A5 的 A3 发现新的提案者 A1 是个小人物，不予理会。

A1 没有达到多数，A5 达到了，于是 A5 将主持投票，决议的内容是 A5 提出的税率 20%.

如果 A3 决定平等地对待每一位议员，对 A1 做出"你之前有人提出将税率定为 20%"的回复，则将造成混乱。这种情况下 A1 和 A5 都将试图主持投票，但是这次两份提案的内容不同。

这种情况下, A3 若对 A1 进行回复，只能说：

```
有更大的人物关注此事，请等待他做出决定。
```

另外，在这种情况下, A4 与外界失去了联系。等到他恢复联系，并需要得知税率情况时，他(在最简单的协议中)将提出一个提案：

```
现有的税率是什么?如果没有决定，则建议将其定为15%.时间：本届议会第3年4月1日;提案者：A4
```

这时，(在最简单的协议中)其他议员将会回复：

```
税率已在之前的投票中定为20%,你不要再来烦我!
```

# 算法实践

## 基于逻辑时钟的 Paxos 协议

- [使用逻辑时钟重述 paxos 协议](http://www.tuicool.com/articles/MbQjQnB)

为了让这套系统能正确运行，我们需要一个精确的时钟。由于操作系统的物理时钟经常是有偏差的，所以我们决定采用一个逻辑时钟。时钟的目的是给系统中 发生的每一个事件编排一个序号。假设我们有一台单独的机器提供了一个全局的计数器服务。它只支持一个方法：incrementAndGet()。这个方法 的作用是将计数器的值加一，并且返回增加后的值。我们将这个计数器称为 globalClock。globalClock 的初始值为 0。然后，系统中的每个其它机器，都有一个自己的 localClock,它的初始值来自 globalClock。

```
int globalClock::incrementAndGet(){
   return (MAX_SERVER_ID+1)*++localCounter+serverID;
}
```

假设机器和机器之间通过 request-response 这样的模式通讯。每条网络消息的类型要么是 reqeust，要么是 response。response 又分为两种：OK 和 Rejected。无论什么类型的网络消息，都必须带有一个时间戳，时间戳取自这台机器的 localClock。我们规定消息的接收方必须执行以下行为：

```java
if(message.timestamp&localClock && message.type==request) reject(message);
else {
  localClock=message.timestamp;
  process(message);
}
```

简而言之就是：我们不处理过时的请求。并且利用网络间传输的消息作为时钟同步机制。一旦收到过时的请求，就返回 Rejected 类型的答复。另外，我们要求时钟不能倒流。我们必须容忍机器突然 crash。在机器重新起来之后，要么 localClock 从 globalClock 取一个新值，要么 localClock 每次更新的时候都必须写入到本地硬盘上，重启之后从那再读进来。我们偏向于第二种方案，因为我后面将会讲怎么去掉 globalClock 这个服务器。

Paxos 协议分为两个阶段。

1. 设置时钟：proposer 令 localClock=globalClock.incrementAndGet()。
2. prepare:proposer 向所有 Acceptor 发送一个 prepare 消息。接收方应返回它最近一次 accept 的 value，以及 accept 的时间，若在它还没有 accept 过 value，那么就返回空。proposer 只有在收到过半数的 response 之后，才可进入下一个阶段。一旦收到 reject 消息，那么就重头来。
3. 构造 Proposal:proposer 从 prepare 阶段收到的所有 values 中选取时间戳最新的一个。如果没有，那么它自己提议一个 value。
4. 发送 Proposal:proposer 把 value 发送给其它所有机器，消息的时间戳取自 localClock。接收方只要检查消息时间戳合法，那么就接受此 value，把这个 value 和时间戳写入到硬盘上，然后答复 OK，否则拒绝接受。proposer 若收到任何的 reject 答复，则回到 step1。否则，在收到过半数的 OK 后，此 Proposal 被通过。

## Leader 选举实例

这里具体例子来说明 Paxos 的整个具体流程: 假如有 Server1、Server2、Server3 这样三台服务器，我们要从中选出 leader，这时候 Paxos 派上用场了。整个选举的结构图如下：

![enter image description here](http://www.solinx.co/wp-content/uploads/2015/10/Paxos.png)

### Phase1(准备阶段)

1. 每个 Server 都向 Proposer 发消息称自己要成为 leader，Server1 往 Proposer1 发、Server2 往 Proposer2 发、Server3 往 Proposer3 发；
2. 现在每个 Proposer 都接收到了 Server1 发来的消息但时间不一样，Proposer2 先接收到了，然后是 Proposer1，接着才是 Proposer3；
3. Proposer2 首先接收到消息所以他从系统中取得一个编号 1，Proposer2 向 Acceptor2 和 Acceptor3 发送一条，编号为 1 的消息；接着 Proposer1 也接收到了 Server1 发来的消息，取得一个编号 2，Proposer1 向 Acceptor1 和 Acceptor2 发送一条，编号为 2 的消息；最后 Proposer3 也接收到了 Server3 发来的消息，取得一个编号 3，Proposer3 向 Acceptor2 和 Acceptor3 发送一条，编号为 3 的消息；
4. 这时 Proposer1 发送的消息先到达 Acceptor1 和 Acceptor2，这两个都没有接收过请求所以接受了请求返回[2,null]给 Proposer1，并承诺不接受编号小于 2 的请求；
5. 此时 Proposer2 发送的消息到达 Acceptor2 和 Acceptor3，Acceprot3 没有接收过请求返回[1,null]给 Proposer2，并承诺不接受编号小于 1 的请求，但这时 Acceptor2 已经接受过 Proposer1 的请求并承诺不接受编号小于的 2 的请求了，所以 Acceptor2 拒绝 Proposer2 的请求；
6. 最后 Proposer3 发送的消息到达 Acceptor2 和 Acceptor3，Acceptor2 接受过提议，但此时编号为 3 大于 Acceptor2 的承诺 2 与 Accetpor3 的承诺 1，所以接受提议返回[3,null];
7. Proposer2 没收到过半的回复所以重新取得编号 4，并发送给 Acceptor2 和 Acceptor3，然后 Acceptor2 和 Acceptor3

### Phase2(决议阶段)

1. Proposer3 收到过半(三个 Server 中两个)的返回，并且返回的 Value 为 null，所以 Proposer3 提交了[3,server3]的议案；
2. Proposer1 收到过半返回，返回的 Value 为 null，所以 Proposer1 提交了[2,server1]的议案；
3. Proposer2 收到过半返回，返回的 Value 为 null，所以 Proposer2 提交了[4,server2]的议案；
4. Acceptor1、Acceptor2 接收到 Proposer1 的提案[2,server1]请求，Acceptor2 承诺编号大于 4 所以拒绝了通过，Acceptor1 通过了请求；
5. Proposer2 的提案[4,server2]发送到了 Acceptor2、Acceptor3，提案编号为 4 所以 Acceptor2、Acceptor3 都通过了提案请求；
6. Acceptor2、Acceptor3 接收到 Proposer3 的提案[3,server3]请求，Acceptor2、Acceptor3 承诺编号大于 4 所以拒绝了提案；
7. 此时过半的 Acceptor 都接受了 Proposer2 的提案[4,server2],Larner 感知到了提案的通过，Larner 学习提案，server2 成为 Leader；

一个 Paxos 过程只会产生一个议案所以至此这个流程结束，选举结果 server2 为 Leader；
