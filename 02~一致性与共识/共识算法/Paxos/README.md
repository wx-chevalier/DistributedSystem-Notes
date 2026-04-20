# Paxos

Google Chubby 的作者 Mike Burrows 说过这个世界上只有一种一致性算法，那就是 Paxos，其它的算法都是残次品。

```
--- Paxos Proposer ---

1  proposer(v):
2    while not decided:
2      choose n, unique and higher than any n seen so far
3      send prepare(n) to all servers including self
4      if prepare_ok(n, na, va) from majority:
5        v' = va with highest na; choose own v otherwise
6        send accept(n, v') to all
7        if accept_ok(n) from majority:
8          send decided(v') to all


--- Paxos Acceptor ---

9  acceptor state on each node (persistent):
10   np     --- highest prepare seen
11   na, va --- highest accept seen

12  acceptor's prepare(n) handler:
13   if n > np
14     np = n
15     reply prepare_ok(n, na, va)
16   else
17     reply prepare_reject


18  acceptor's accept(n, v) handler:
19   if n >= np
20     np = n
21     na = n
22     va = v
23     reply accept_ok(n)
24   else
25     reply accept_reject

```
