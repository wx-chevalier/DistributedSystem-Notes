# TCP

我们首先要了解 TCP 报文组成，根据 TCP 报文组成格式，重点了解几个重要的字段有助于我们在后面分析 TCP/IP 报文。

![](http://www.2cto.com/uploadfile/2013/1022/20131022025345890.png)

上图中有几个字段需要重点介绍下：(1)序号：Seq 序号，占 32 位，用来标识从 TCP 源端向目的端发送的字节流，发起方发送数据时对此进行标记。(2)确认序号：Ack 序号，占 32 位，只有 ACK 标志位为 1 时，确认序号字段才有效，Ack=Seq+1。(3)标志位：共 6 个，即 URG、ACK、PSH、RST、SYN、FIN 等，具体含义如下：(A)URG：紧急指针(urgent pointer)有效。(B)ACK：确认序号有效。(C)PSH：接收方应该尽快将这个报文交给应用层。(D)RST：重置连接。(E)SYN：发起一个新连接。(F)FIN：释放一个连接。

需要注意的是：(A)不要将确认序号 Ack 与标志位中的 ACK 搞混了。(B)确认方 Ack=发起方 Req+1，两端配对。

建立 TCP 需要三次握手才能建立，而断开连接则需要四次握手。整个过程如下图所示：
![](http://hi.csdn.net/attachment/201108/7/0_131271823564Rx.gif)

# 层次化模型

# 连接建立与关闭

## 建立连接:三次握手

TCP 建立连接的过程简单来说，首先 Client 端发送连接请求报文，Server 段接受连接后回复 ACK 报文，并为这次连接分配资源。Client 端接收到 ACK 报文后也向 Server 段发生 ACK 报文，并分配资源，这样 TCP 连接就建立了。TCP 建立连接时，首先客户端和服务器处于 close 状态。然后客户端发送 SYN 同步位，此时客户端处于 SYN-SEND 状态，服务器处于 lISTEN 状 态，当服务器收到 SYN 以后，向客户端发送同步位 SYN 和确认码 ACK，然后服务器变为 SYN-RCVD，客户端收到服务器发来的 SYN 和 ACK 后，客户 端的状态变成 ESTABLISHED(已建立连接)，客户端再向服务器发送 ACK 确认码，服务器接收到以后也变成 ESTABLISHED。然后服务器客户 端开始数据传输

![](https://coding.net/u/hoteam/p/Cache/git/raw/master/2016/8/2/tcp-three-way-handshake-four-wave.png)

- 第一次握手：Client 将标志位 SYN 置为 1，随机产生一个值 seq=J，并将该数据包发送给 Server，Client 进入 SYN_SENT 状态，等待 Server 确认。
- 第二次握手：Server 收到数据包后由标志位 SYN=1 知道 Client 请求建立连接，Server 将标志位 SYN 和 ACK 都置为 1，ack=J+1，随机产生一个值 seq=K，并将该数据包发送给 Client 以确认连接请求，Server 进入 SYN_RCVD 状态。
- 第三次握手：Client 收到确认后，检查 ack 是否为 J+1，ACK 是否为 1，如果正确则将标志位 ACK 置为 1，ack=K+1，并将该数据包发 送给 Server，Server 检查 ack 是否为 K+1，ACK 是否为 1，如果正确则连接建立成功，Client 和 Server 进入 ESTABLISHED 状态，完成三次握手，随后 Client 与 Server 之间可以开始传输数据了。

## 关闭连接:四次握手

关闭连接的四次握手过程可以概述为，设 Client 端发起中断连接请求，也就是发送 FIN 报文。Server 端接到 FIN 报文后，意思是说"我 Client 端没有数据要发给你了"，但是如果你还有数据没有发送完成，则不必急着关闭 Socket，可以继续发送数据。所以你先发送 ACK，"告诉 Client 端，你的请求我收到了，但是我还没准备好，请继续你等我的消息"。这个时候 Client 端就进入 FIN_WAIT 状态，继续等待 Server 端的 FIN 报文。当 Server 端确定数据已发送完成，则向 Client 端发送 FIN 报文，"告诉 Client 端，好了，我这边数据发完了，准备好关闭连接了"。Client 端收到 FIN 报文后，"就知道可以关闭连接了，但是他还是不相信网络，怕 Server 端不知道要关闭，所以发送 ACK 后进入 TIME_WAIT 状态，如果 Server 端没有收到 ACK 则可以重传。“，Server 端收到 ACK 后，"就知道可以断开连接了"。Client 端等待了 2MSL 后依然没有收到回复，则证明 Server 端已正常关闭，那好，我 Client 端也可以关闭连接了。Ok，TCP 连接就这样关闭了！

![](https://coding.net/u/hoteam/p/Cache/git/raw/master/2016/8/2/tcp-three-way-handshake-four-wave_1_thumb.png)

- 第一次挥手：Client 发送一个 FIN，用来关闭 Client 到 Server 的数据传送，Client 进入 FIN_WAIT_1 状态。

- 第二次挥手：Server 收到 FIN 后，发送一个 ACK 给 Client，确认序号为收到序号+1(与 SYN 相同，一个 FIN 占用一个序号)，Server 进入 CLOSE_WAIT 状态。

- 第三次挥手：Server 发送一个 FIN，用来关闭 Server 到 Client 的数据传送，Server 进入 LAST_ACK 状态。

- 第四次挥手：Client 收到 FIN 后，Client 进入 TIME_WAIT 状态，接着发送一个 ACK 给 Server，确认序号为收到序号+1，Server 进入 CLOSED 状态，完成四次挥手。

由于 TCP 连接时全双工的，因此，每个方向都必须要单独进行关闭，这一原则是当一方完成数据发送任务后，发送一个 FIN 来终止这一方向的连接，收到一个 FIN 只是意味着这一方向上没有数据流动了，即不会再收到数据了，但是在这个 TCP 连接上仍然能够发送数据，直到这一方向也发送了 FIN。首先进行关闭的 一方将执行主动关闭，而另一方则执行被动关闭。

![](http://www.2cto.com/uploadfile/2013/1022/20131022025351387.png)

# 地址与协议

## localhost 与 127.0.0.1

Pv4 的网络标准把 从 127.0.0.1 到 127.255.255.254 IP 地址块都用作 loopback 。所有的发到这些地址的数据包都会被毫发无损的返回去(looped back )，这一千六百多万个个地址中，最知名的、最常用的就是 127.0.0.1 。对于 IPv6 来说，它只把一个地址用作 loopback , 就是::1 (0000:0000:0000:0000:0000:0000:0000:0001) 。有了 loopback 地址，同一个计算机上的进程通信都很方便了，根本不用走实际的物理网卡。比如说你在本机建立了一个 Web 服务器，然后通过浏览器用http://127.0.0.1:8080 去访问，操作系统内的网络协议栈会把这个 HTTP GET 请求封装到一个 TCP 包中，写上目的端口号 8080，然后再封装到一个 IP 包中，写上目的地址 127.0.0.1 。

但是这个 IP 数据包并不会发送到物理的网卡那里去，更不会通过数据链路层发送到局域网乃至互联网中，实际上它发给了虚拟的网络接口, 然后立刻被 looped back 到 IP 层的输入队列中。

IP 层收到数据包，交付给 TCP 层，TCP 层发现目的端口是 8080，就会把 GET 请求取出来，交付给绑定 8080 端口的 Web 服务器。

在 Unix 和 Linux 系统中，通过把 loopback 接口命名为 lo 或者 lo0 (注意第一个字母是 L 的小写字母，不是数字一)

至于 localhost , 这就是个本机的主机名，在大多数机器上，这个主机名都会被计算机操作系统映射到 127.0.0.1 (ipv4)或者::1 (ipv6) , 那使用 localhost 和 ip 实际上一样了。

```
127.0.0.1 localhost
::1 localhost
```

但是有个有意思的例外就是 mysql , 在 Linux 上，当你使用 localhost 来连接数据库的时候，Mysql 会使用 Unix domain socket 来传输数据，这种方式会快一些，因为这是一种进程内通信(IPC)机制，不走网络协议栈，不需要打包拆包，计算校验和，维护序号等操作。当你使用 127.0.0.1 的时候，mysql 还是会使用 TCP/IP 协议栈来进行数据传输。
