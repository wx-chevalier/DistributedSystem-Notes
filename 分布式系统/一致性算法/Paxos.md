# Paxos

Google Chubby 的作者 Mike Burrows 说过这个世界上只有一种一致性算法，那就是 Paxos，其它的算法都是残次品。Paxos 可以用来解决分布式环境下，选举(或设置)某一个值的问题(比如更新数据库中某个 user 的 age 是多少)。注意，在 Paxos 算法中是不考虑拜占庭将军问题的。

分布式系统中有多个节点就会存在节点间通信的问题，存在着两种节点通讯模型：共享内存(Shared memory)、消息传递(Messages passing)，Paxos 是基于消息传递的通讯模型的。它的假设前提是，在分布式系统中进程之间的通信会出现丢失、延迟、重复等现象，但不会出现传错的现象。Paxos 算法就是为了保证在这样的系统中进程间基于消息传递就某个值达成一致。

# 基础概念

## Scenario | 场景介绍

比如下面的情况：有三个服务器进程 A,B,C 运行在三台主机上提供数据库服务，当一个 client 连接到任何一台服务器上的时候，都能通过该服务器进行 read 和 update 的操作。

首先先看一个 A、B、C 不那么均等的方法：A、B、C 选举主机名最大的服务器为 master 提供服务，所有的 read、update 操作都发生在它上面，其余的两台服务器只是 slave(后者，在这里类似 proxy)，当 client 是连接在 master 的服务器的时候，其直接和 master 交互，就类似于单机的场景。当 client 时连接在 slave 的时候，所有的请求被转移到了 master 上进行操作，然后再结果返回给 client。如果 master 挂了，那么剩余两台主机在检测到 master 挂的情况后，再根据主机名最大的方法选举 master。

上面的算法有一个问题，它让 A、B、C 不那么均等了，A、B、C 存在角色之分，且在某个时候 master 挂机后，需要立刻选举出新的 master 提供服务。同时，这个算法还要求各个服务器之间保持心跳。而 Paxos 算法则不同，Paxos 提供了一种将 A、B、C 等价的方式提供上面的操作，并保证数据的正确性。在某台主机宕机后，只要总数一半以上的服务器还存活，则整个集群依然能对外提供服务，甚至不需要心跳。

## Terminology | 名词解释

为描述 Paxos 算法，Lamport 虚拟了一个叫做 Paxos 的[希腊城邦](https://zh.wikipedia.org/wiki/%E5%B8%8C%E8%87%98%E5%9F%8E%E9%82%A6)，这个岛按照议会民主制的政治模式制订法律，但是没有人愿意将自己的全部时间和精力放在这种事情上。所以无论是议员，议长或者传递纸条的服务员都不能承诺别人需要时一定会出现，也无法承诺批准决议或者传递消息的时间。但是这里假设没有拜占庭将军问题；只要等待足够的时间，消息就会被传到。

另外，Paxos 岛上的议员是不会反对其他议员提出的决议的。首先将议员的角色分为 Proposers，Acceptors，和 Learners(允许身兼数职)。proposers 提出提案，提案信息包括提案编号和提议的 value；Acceptor 收到提案后可以接受(accept)提案，若提案获得多数 Acceptors 的接受，则称该提案被批准(chosen)Learners 只能“学习”被批准的提案。划分角色后，就可以更精确的定义问题：

- 决议(value)只有在被 proposers 提出后才能被批准(未经批准的决议称为“提案(proposal)”)；
- 在一次 Paxos 算法的执行实例中，只批准(chosen)一个 value；
- learners 只能获得被批准(chosen)的 value。

# 算法流程

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
