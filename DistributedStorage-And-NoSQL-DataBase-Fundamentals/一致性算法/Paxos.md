 

# Paxos
> Google Chubby的作者Mike Burrows说过这个世界上只有一种一致性算法，那就是Paxos，其它的算法都是残次品。


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

PAXOS可以用来解决分布式环境下，选举（或设置）某一个值的问题（比如更新数据库中某个user的age是多少）。分布式系统中有多个节点就会存在节点间通信的问题，存在着两种节点通讯模型：共享内存（Shared memory）、消息传递（Messages passing），Paxos是基于消息传递的通讯模型的。它的假设前提是，在分布式系统中进程之间的通信会出现丢失、延迟、重复等现象，但不会出现传错的现象。Paxos算法就是为了保证在这样的系统中进程间基于消息传递就某个值达成一致。
> 注意，在Paxos算法中是不考虑拜占庭将军问题的

## Scenario:场景介绍
比如下面的情况：有三个服务器进程A,B,C运行在三台主机上提供数据库服务，当一个client连接到任何一台服务器上的时候，都能通过该服务器进行read和update的操作。首先先看一个A、B、C不那么均等的方法：
A、B、C选举主机名最大的服务器为master提供服务，所有的read、update操作都发生在它上面，其余的两台服务器只是slave（后者，在这里类似proxy），当client是连接在master的服务器的时候，其直接和master交互，就类似于单机的场景。当client时连接在slave的时候，所有的请求被转移到了master上进行操作，然后再结果返回给client。如果master挂了，那么剩余两台主机在检测到master挂的情况后，再根据主机名最大的方法选举master。
上面的算法有一个问题，它让A、B、C不那么均等了，A、B、C存在角色之分，且在某个时候master挂机后，需要立刻选举出新的master提供服务。同时，这个算法还要求各个服务器之间保持心跳。而PAXOS算法则不同，PAXOS提供了一种将A、B、C等价的方式提供上面的操作，并保证数据的正确性。在某台主机宕机后，只要总数一半以上的服务器还存活，则整个集群依然能对外提供服务，甚至不需要心跳。


## Reference
- [维基百科上关于Paxos算法的讲解](https://zh.wikipedia.org/wiki/Paxos%E7%AE%97%E6%B3%95)
- [Paxos算法简述](http://my.oschina.net/linlifeng/blog/78918)
- [一致性算法Paxos详解](http://www.solinx.co/archives/403)
- [微信PaxosStore：深入浅出Paxos算法协议 ](https://www.sdk.cn/news/5826)
- [微信PaxosStore内存云揭秘：十亿Paxos/分钟的挑战 ](http://mp.weixin.qq.com/s?__biz=MzI4NDMyNTU2Mw==&mid=2247483804&idx=1&sn=a6629ebdaefbc2470c2ecbf12577daff)
- [图解 Paxos 一致性协议](http://blog.xiaohansong.com/2016/09/30/Paxos/)
- [Plain Paxos Implementations in Python & Java](https://github.com/cocagne/paxos)


# 算法原理
## Terminology:名词解释
为描述Paxos算法，Lamport虚拟了一个叫做Paxos的[希腊城邦](https://zh.wikipedia.org/wiki/%E5%B8%8C%E8%87%98%E5%9F%8E%E9%82%A6)，这个岛按照议会民主制的政治模式制订法律，但是没有人愿意将自己的全部时间和精力放在这种事情上。所以无论是议员，议长或者传递纸条的服务员都不能承诺别人需要时一定会出现，也无法承诺批准决议或者传递消息的时间。但是这里假设没有[拜占庭将军问题](https://zh.wikipedia.org/wiki/%E6%8B%9C%E5%8D%A0%E5%BA%AD%E5%B0%86%E5%86%9B%E9%97%AE%E9%A2%98)（Byzantine failure，即虽然有可能一个消息被传递了两次，但是绝对不会出现错误的消息）；只要等待足够的时间，消息就会被传到。另外，Paxos岛上的议员是不会反对其他议员提出的决议的。
首先将议员的角色分为proposers，Acceptors，和learners（允许身兼数职）。proposers提出提案，提案信息包括提案编号和提议的value；Acceptor收到提案后可以接受（accept）提案，若提案获得多数Acceptors的接受，则称该提案被批准（chosen）；learners只能“学习”被批准的提案。划分角色后，就可以更精确的定义问题：
1. 决议（value）只有在被proposers提出后才能被批准（未经批准的决议称为“提案（proposal）”）；
2. 在一次Paxos算法的执行实例中，只批准（chosen）一个value；
3. learners只能获得被批准（chosen）的value。
## Process:处理流程
批准value的过程中，首先proposers将value发送给Acceptors，之后Acceptors对value进行接受（accept）。为了满足只批准一个value的约束，要求经“多数派（majority）”接受的value成为正式的决议（称为“批准”决议）。这是因为无论是按照人数还是按照权重划分，两组“多数派”至少有一个公共的Acceptor，如果每个Acceptor只能接受一个value，约束2就能保证。整个过程（一个实例或称一个事务或一个Round）分为两个阶段：
- phase1（准备阶段）
    - Proposer向超过半数（n/2+1）Acceptor发起prepare消息(发送编号)
    - 如果prepare符合协议规则Acceptor回复promise消息，否则拒绝
- phase2（决议阶段或投票阶段）
    - 如果超过半数Acceptor回复promise，Proposer向Acceptor发送accept消息(此时包含真实的值)
    - Acceptor检查accept消息是否符合规则，消息符合则批准accept请求

根据上述过程当一个proposer发现存在编号更大的提案时将终止提案。这意味着提出一个编号更大的提案会终止之前的提案过程。如果两个proposer在这种情况下都转而提出一个编号更大的提案，就可能陷入活锁，违背了Progress的要求。这种情况下的解决方案是选举出一个leader，仅允许leader提出提案。但是由于消息传递的不确定性，可能有多个proposer自认为自己已经成为leader。Lamport在The Part-Time Parliament一文中描述并解决了这个问题。
### 约束条件
##### P1： Acceptor必须接受他接收到的第一个提案，
注意P1是不完备的。如果恰好一半Acceptor接受的提案具有value A，另一半接受的提案具有value B，那么就无法形成多数派，无法批准任何一个value。
##### P2：当且仅当Acceptor没有回应过编号大于n的prepare请求时，Acceptor接受（accept）编号为n的提案。
如果一个没有chosen过任何proposer提案的Acceptor在prepare过程中回答了一个proposer针对提案n的问题，但是在开始对n进行投票前，又接受（accept）了编号小于n的另一个提案（例如n-1），如果n-1和n具有不同的value，这个投票就会违背P2c。因此在prepare过程中，Acceptor进行的回答同时也应包含承诺：不会再接受（accept）编号小于n的提案。
##### P3：只有Acceptor没有接受过提案Proposer才能采用自己的Value，否者Proposer的Value提案为Acceptor中编号最大的Proposer Value；
##### P4: 一个提案被选中需要过半数的Acceptor接受。
假设A为整个Acceptor集合，B为一个超过A一半的Acceptor集合，B为A的子集，C也是一个超过A一半的Acceptor集合，C也是A的子集，有此可知任意两个过半集合中必定有一个共同的成员Acceptor；此说明了一个Acceptor可以接受不止一个提案，此时需要一个编号来标识每一个提案，提案的格式为：[编号，Value]，编号为不可重复全序的，因为存在着一个一个Paxos过程只能批准一个value这时又推出了一个约束P3；
##### P5：当编号K0、Value为V0的提案(即[K0,V0])被过半的Acceptor接受后，今后（同一个Paxos或称一个Round中）所有比K0更高编号且被Acceptor接受的提案，其Value值也必须为V0。
该约束还可以表述为：
- 一旦一个具有value v的提案被批准（chosen），那么之后任何Acceptor再次接受（accept）的提案必须具有value v。
- 一旦一个具有value v的提案被批准（chosen），那么以后任何proposer提出的提案必须具有value v。
- 如果一个编号为n的提案具有value v，那么存在一个多数派，要么他们中所有人都没有接受（accept）编号小于n 
的任何提案，要么他们已经接受（accept）的所有编号小于n的提案中编号最大的那个提案具有value v。

因为每个Proposer都可提出多个议案，每个议案最初都有一个不同的Value所以要满足P3就又要推出一个新的约束P4；

### 决议的发布
一个显而易见的方法是当acceptors批准一个value时，将这个消息发送给所有learner。但是这个方法会导致消息量过大。

由于假设没有Byzantine failures，learners可以通过别的learners获取已经通过的决议。因此acceptors只需将批准的消息发送给指定的某一个learner，其他learners向它询问已经通过的决议。这个方法降低了消息量，但是指定learner失效将引起系统失效。

因此acceptors需要将accept消息发送给learners的一个子集，然后由这些learners去通知所有learners。

但是由于消息传递的不确定性，可能会没有任何learner获得了决议批准的消息。当learners需要了解决议通过情况时，可以让一个proposer重新进行一次提案。注意一个learner可能兼任proposer。


### 议员税率问题实例
有A1, A2, A3, A4, A5 5位议员，就税率问题进行决议。议员A1决定将税率定为10%,因此它向所有人发出一个草案。这个草案的内容是：

```
现有的税率是什么?如果没有决定，则建议将其定为10%.时间：本届议会第3年3月15日;提案者：A1

```

在最简单的情况下，没有人与其竞争;信息能及时顺利地传达到其它议员处。

于是, A2-A5回应：

```
我已收到你的提案，等待最终批准

```

而A1在收到2份回复后就发布最终决议：

```
税率已定为10%,新的提案不得再讨论本问题。

```

这实际上退化为[二阶段提交](https://zh.wikipedia.org/wiki/%E4%BA%8C%E9%98%B6%E6%AE%B5%E6%8F%90%E4%BA%A4)协议。

现在我们假设在A1提出提案的同时, A5决定将税率定为20%:

```
现有的税率是什么?如果没有决定，则建议将其定为20%.时间：本届议会第3年3月15日;提案者：A5

```

草案要通过侍从送到其它议员的案头. A1的草案将由4位侍从送到A2-A5那里。现在，负责A2和A3的侍从将草案顺利送达，负责A4和A5的侍从则不上班. A5的草案则顺利的送至A3和A4手中。

现在, A1, A2, A3收到了A1的提案; A3, A4, A5收到了A5的提案。按照协议, A1, A2, A4, A5将接受他们收到的提案，侍从将拿着

```
我已收到你的提案，等待最终批准

```

的回复回到提案者那里。

而A3的行为将决定批准哪一个。

#### 情况一:仅有单议员提出议案

假设A1的提案先送到A3处，而A5的侍从决定放假一段时间。于是A3接受并派出了侍从. A1等到了两位侍从，加上它自己已经构成一个多数派，于是税率10%将成为决议. A1派出侍从将决议送到所有议员处：

```
税率已定为10%,新的提案不得再讨论本问题。

```

A3在很久以后收到了来自A5的提案。由于税率问题已经讨论完毕，他决定不再理会。但是他要抱怨一句：

```
税率已在之前的投票中定为10%,你不要再来烦我!

```

这个回复对A5可能有帮助，因为A5可能因为某种原因很久无法与与外界联系了。当然更可能对A5没有任何作用，因为A5可能已经从A1处获得了刚才的决议。

#### 情况二:多议员提出议案，但是第一份议案在第二份议案广播前形成决议

依然假设A1的提案先送到A3处，但是这次A5的侍从不是放假了，只是中途耽搁了一会。这次, A3依然会将"接受"回复给A1.但是在决议成型之前它又收到了A5的提案。这时协议有两种处理方式：

1.如果A5的提案更早，按照传统应该由较早的提案者主持投票。现在看来两份提案的时间一样（本届议会第3年3月15日）。但是A5是个惹不起的大人物。于是A3回复：

```
我已收到您的提案，等待最终批准，但是您之前有人提出将税率定为10%,请明察。

```

于是, A1和A5都收到了足够的回复。这时关于税率问题就有两个提案在同时进行。但是A5知道之前有人提出税率为10%.于是A1和A5都会向全体议员广播：

```
 税率已定为10%,新的提案不得再讨论本问题。

```

一致性得到了保证。

\2. A5是个无足轻重的小人物。这时A3不再理会他, A1不久后就会广播税率定为10%.

#### 情况三:多议员同时提出议案

在这个情况中，我们将看见，根据提案的时间及提案者的权势决定是否应答是有意义的。在这里，时间和提案者的权势就构成了给提案编号的依据。这样的编号符合"任何两个提案之间构成偏序"的要求。

A1和A5同样提出上述提案，这时A1可以正常联系A2和A3; A5也可以正常联系这两个人。这次A2先收到A1的提案; A3则先收到A5的提案. A5更有权势。

在这种情况下，已经回答A1的A2发现有比A1更有权势的A5提出了税率20%的新提案，于是回复A5说：

```
我已收到您的提案，等待最终批准。
```

而回复了A5的A3发现新的提案者A1是个小人物，不予理会。

A1没有达到多数，A5达到了，于是A5将主持投票，决议的内容是A5提出的税率20%.

如果A3决定平等地对待每一位议员，对A1做出"你之前有人提出将税率定为20%"的回复，则将造成混乱。这种情况下A1和A5都将试图主持投票，但是这次两份提案的内容不同。

这种情况下, A3若对A1进行回复，只能说：

```
有更大的人物关注此事，请等待他做出决定。
```

另外，在这种情况下, A4与外界失去了联系。等到他恢复联系，并需要得知税率情况时，他（在最简单的协议中）将提出一个提案：

```
现有的税率是什么?如果没有决定，则建议将其定为15%.时间：本届议会第3年4月1日;提案者：A4
```

这时，（在最简单的协议中）其他议员将会回复：

```
税率已在之前的投票中定为20%,你不要再来烦我!
```

# 算法实践
## 基于逻辑时钟的Paxos协议
- [使用逻辑时钟重述paxos协议](http://www.tuicool.com/articles/MbQjQnB)

为了让这套系统能正确运行，我们需要一个精确的时钟。由于操作系统的物理时钟经常是有偏差的，所以我们决定采用一个逻辑时钟。时钟的目的是给系统中 发生的每一个事件编排一个序号。假设我们有一台单独的机器提供了一个全局的计数器服务。它只支持一个方法：incrementAndGet()。这个方法 的作用是将计数器的值加一，并且返回增加后的值。我们将这个计数器称为globalClock。globalClock的初始值为0。然后，系统中的每个其它机器，都有一个自己的localClock,它的初始值来自globalClock。
```
int globalClock::incrementAndGet(){
   return (MAX_SERVER_ID+1)*++localCounter+serverID;
}
```
假设机器和机器之间通过request-response这样的模式通讯。每条网络消息的类型要么是reqeust，要么是response。response又分为两种：OK和Rejected。无论什么类型的网络消息，都必须带有一个时间戳，时间戳取自这台机器的localClock。我们规定消息的接收方必须执行以下行为：

```java
if(message.timestamp&localClock && message.type==request) reject(message);
else {
  localClock=message.timestamp; 
  process(message);
}
```
简而言之就是：我们不处理过时的请求。并且利用网络间传输的消息作为时钟同步机制。一旦收到过时的请求，就返回Rejected类型的答复。另外，我们要求时钟不能倒流。我们必须容忍机器突然crash。在机器重新起来之后，要么localClock从globalClock取一个新值，要么localClock每次更新的时候都必须写入到本地硬盘上，重启之后从那再读进来。我们偏向于第二种方案，因为我后面将会讲怎么去掉 globalClock这个服务器。

Paxos协议分为两个阶段。
1. 设置时钟：proposer令localClock=globalClock.incrementAndGet()。
2. prepare:proposer向所有Acceptor发送一个prepare消息。接收方应返回它最近一次accept的value，以及 accept的时间，若在它还没有accept过value，那么就返回空。proposer只有在收到过半数的response之后，才可进入下一个阶段。一旦收到reject消息，那么就重头来。
3. 构造Proposal:proposer从prepare阶段收到的所有values中选取时间戳最新的一个。如果没有，那么它自己提议一个value。
4. 发送Proposal:proposer把value发送给其它所有机器，消息的时间戳取自localClock。接收方只要检查消息时间戳合法，那么就接受此value，把这个value和时间戳写入到硬盘上，然后答复OK，否则拒绝接受。proposer若收到任何的reject答复，则回到 step1。否则，在收到过半数的OK后，此Proposal被通过。

## Leader选举实例
这里具体例子来说明Paxos的整个具体流程： 假如有Server1、Server2、Server3这样三台服务器，我们要从中选出leader，这时候Paxos派上用场了。整个选举的结构图如下：

![enter image description here](http://www.solinx.co/wp-content/uploads/2015/10/Paxos.png)


### **Phase1（准备阶段）**
1. 每个Server都向Proposer发消息称自己要成为leader，Server1往Proposer1发、Server2往Proposer2发、Server3往Proposer3发；
2. 现在每个Proposer都接收到了Server1发来的消息但时间不一样，Proposer2先接收到了，然后是Proposer1，接着才是Proposer3；
3. Proposer2首先接收到消息所以他从系统中取得一个编号1，Proposer2向Acceptor2和Acceptor3发送一条，编号为1的消
息；接着Proposer1也接收到了Server1发来的消息，取得一个编号2，Proposer1向Acceptor1和Acceptor2发送一条，编号为2的消息； 最后Proposer3也接收到了Server3发来的消息，取得一个编号3，Proposer3向Acceptor2和Acceptor3发送一条，编
号为3的消息；
4. 这时Proposer1发送的消息先到达Acceptor1和Acceptor2，这两个都没有接收过请求所以接受了请求返回[2,null]给Proposer1，并承诺不接受编号小于2的请求；
5. 此时Proposer2发送的消息到达Acceptor2和Acceptor3，Acceprot3没有接收过请求返回[1,null]给Proposer2，并承诺不接受编号小于1的请求，但这时Acceptor2已经接受过Proposer1的请求并承诺不接受编号小于的2的请求了，所以Acceptor2拒绝Proposer2的请求；
6. 最后Proposer3发送的消息到达Acceptor2和Acceptor3，Acceptor2接受过提议，但此时编号为3大于Acceptor2的承诺2与Accetpor3的承诺1，所以接受提议返回[3,null];
7. Proposer2没收到过半的回复所以重新取得编号4，并发送给Acceptor2和Acceptor3，然后Acceptor2和Acceptor3


### **Phase2（决议阶段）**
1. Proposer3收到过半（三个Server中两个）的返回，并且返回的Value为null，所以Proposer3提交了[3,server3]的议案；
2. Proposer1收到过半返回，返回的Value为null，所以Proposer1提交了[2,server1]的议案；
3. Proposer2收到过半返回，返回的Value为null，所以Proposer2提交了[4,server2]的议案；
4. Acceptor1、Acceptor2接收到Proposer1的提案[2,server1]请求，Acceptor2承诺编号大于4所以拒绝了通过，Acceptor1通过了请求；
5. Proposer2的提案[4,server2]发送到了Acceptor2、Acceptor3，提案编号为4所以Acceptor2、Acceptor3都通过了提案请求；
6. Acceptor2、Acceptor3接收到Proposer3的提案[3,server3]请求，Acceptor2、Acceptor3承诺编号大于4所以拒绝了提案；
7. 此时过半的Acceptor都接受了Proposer2的提案[4,server2],Larner感知到了提案的通过，Larner学习提案，server2成为Leader；

**一个Paxos过程只会产生一个议案所以至此这个流程结束，选举结果server2为Leader；**



