


# TCP 
我们首先要了解TCP报文组成，根据TCP报文组成格式，重点了解几个重要的字段有助于我们在后面分析TCP/IP报文。
![](http://www.2cto.com/uploadfile/2013/1022/20131022025345890.png)
  上图中有几个字段需要重点介绍下：
  （1）序号：Seq序号，占32位，用来标识从TCP源端向目的端发送的字节流，发起方发送数据时对此进行标记。
  （2）确认序号：Ack序号，占32位，只有ACK标志位为1时，确认序号字段才有效，Ack=Seq+1。
  （3）标志位：共6个，即URG、ACK、PSH、RST、SYN、FIN等，具体含义如下：
  （A）URG：紧急指针（urgent pointer）有效。
  （B）ACK：确认序号有效。
  （C）PSH：接收方应该尽快将这个报文交给应用层。
  （D）RST：重置连接。
  （E）SYN：发起一个新连接。
  （F）FIN：释放一个连接。

 需要注意的是：
  （A）不要将确认序号Ack与标志位中的ACK搞混了。
  （B）确认方Ack=发起方Req+1，两端配对。

## Reference
- [localhost vs 127.0.0.1 ](http://mp.weixin.qq.com/s?__biz=MzAxOTc0NzExNg==&mid=2665513390&idx=1&sn=bf0715c8693f14cfbf5fd09737fa4845&chksm=80d679edb7a1f0fb30630fa9816cc307445d87827367f1a7ac0271a28e0279171bce9e558d82#rd)
建立TCP需要三次握手才能建立，而断开连接则需要四次握手。整个过程如下图所示：
![](http://hi.csdn.net/attachment/201108/7/0_131271823564Rx.gif)

# 连接建立与关闭

## 建立连接:三次握手

TCP建立连接的过程简单来说，首先Client端发送连接请求报文，Server段接受连接后回复ACK报文，并为这次连接分配资源。Client端接收到ACK报文后也向Server段发生ACK报文，并分配资源，这样TCP连接就建立了。TCP建立连接时，首先客户端和服务器处于close状态。然后客户端发送SYN同步位，此时客户端处于SYN-SEND状态，服务器处于lISTEN状 态，当服务器收到SYN以后，向客户端发送同步位SYN和确认码ACK，然后服务器变为SYN-RCVD，客户端收到服务器发来的SYN和ACK后，客户 端的状态变成ESTABLISHED(已建立连接)，客户端再向服务器发送ACK确认码，服务器接收到以后也变成ESTABLISHED。然后服务器客户 端开始数据传输
 

![](https://coding.net/u/hoteam/p/Cache/git/raw/master/2016/8/2/tcp-three-way-handshake-four-wave.png)

- 第一次握手：Client将标志位SYN置为1，随机产生一个值seq=J，并将该数据包发送给Server，Client进入SYN_SENT状态，等待Server确认。
- 第二次握手：Server收到数据包后由标志位SYN=1知道Client请求建立连接，Server将标志位SYN和ACK都置为 1，ack=J+1，随机产生一个值seq=K，并将该数据包发送给Client以确认连接请求，Server进入SYN_RCVD状态。
- 第三次握手：Client收到确认后，检查ack是否为J+1，ACK是否为1，如果正确则将标志位ACK置为1，ack=K+1，并将该数据包发 送给Server，Server检查ack是否为K+1，ACK是否为1，如果正确则连接建立成功，Client和Server进入 ESTABLISHED状态，完成三次握手，随后Client与Server之间可以开始传输数据了。

## 关闭连接:四次握手

关闭连接的四次握手过程可以概述为，设Client端发起中断连接请求，也就是发送FIN报文。Server端接到FIN报文后，意思是说"我Client端没有数据要发给你了"，但是如果你还有数据没有发送完成，则不必急着关闭Socket，可以继续发送数据。所以你先发送ACK，"告诉Client端，你的请求我收到了，但是我还没准备好，请继续你等我的消息"。这个时候Client端就进入FIN_WAIT状态，继续等待Server端的FIN报文。当Server端确定数据已发送完成，则向Client端发送FIN报文，"告诉Client端，好了，我这边数据发完了，准备好关闭连接了"。Client端收到FIN报文后，"就知道可以关闭连接了，但是他还是不相信网络，怕Server端不知道要关闭，所以发送ACK后进入TIME_WAIT状态，如果Server端没有收到ACK则可以重传。“，Server端收到ACK后，"就知道可以断开连接了"。Client端等待了2MSL后依然没有收到回复，则证明Server端已正常关闭，那好，我Client端也可以关闭连接了。Ok，TCP连接就这样关闭了！


![](https://coding.net/u/hoteam/p/Cache/git/raw/master/2016/8/2/tcp-three-way-handshake-four-wave_1_thumb.png)

- 第一次挥手：Client发送一个FIN，用来关闭Client到Server的数据传送，Client进入FIN_WAIT_1状态。
- 第二次挥手：Server收到FIN后，发送一个ACK给Client，确认序号为收到序号+1（与SYN相同，一个FIN占用一个序号），Server进入CLOSE_WAIT状态。
- 第三次挥手：Server发送一个FIN，用来关闭Server到Client的数据传送，Server进入LAST_ACK状态。
- 第四次挥手：Client收到FIN后，Client进入TIME_WAIT状态，接着发送一个ACK给Server，确认序号为收到序号+1，Server进入CLOSED状态，完成四次挥手。

由于TCP连接时全双工的，因此，每个方向都必须要单独进行关闭，这一原则是当一方完成数据发送任务后，发送一个FIN来终止这一方向的连接，收到一个 FIN只是意味着这一方向上没有数据流动了，即不会再收到数据了，但是在这个TCP连接上仍然能够发送数据，直到这一方向也发送了FIN。首先进行关闭的 一方将执行主动关闭，而另一方则执行被动关闭。
![](http://www.2cto.com/uploadfile/2013/1022/20131022025351387.png)

# TCP Security
## SYN攻击
在三次握手过程中，Server发送SYN-ACK之后，收到Client的ACK之前的TCP连接称为半连接（half-open connect），此时Server处于SYN_RCVD状态，当收到ACK后，Server转入ESTABLISHED状态。SYN攻击就是 Client在短时间内伪造大量不存在的IP地址，并向Server不断地发送SYN包，Server回复确认包，并等待Client的确认，由于源地址 是不存在的，因此，Server需要不断重发直至超时，这些伪造的SYN包将产时间占用未连接队列，导致正常的SYN请求因为队列满而被丢弃，从而引起网 络堵塞甚至系统瘫痪。SYN攻击时一种典型的DDOS攻击，检测SYN攻击的方式非常简单，即当Server上有大量半连接状态且源IP地址是随机的，则可以断定遭到SYN攻击了，使用如下命令可以让之现行：
```
# netstat -nap | grep SYN_RECV
```

