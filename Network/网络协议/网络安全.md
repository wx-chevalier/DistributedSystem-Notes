# TCP Security

## SYN 攻击

在三次握手过程中，Server 发送 SYN-ACK 之后，收到 Client 的 ACK 之前的 TCP 连接称为半连接(half-open connect)，此时 Server 处于 SYN_RCVD 状态，当收到 ACK 后，Server 转入 ESTABLISHED 状态。SYN 攻击就是 Client 在短时间内伪造大量不存在的 IP 地址，并向 Server 不断地发送 SYN 包，Server 回复确认包，并等待 Client 的确认，由于源地址 是不存在的，因此，Server 需要不断重发直至超时，这些伪造的 SYN 包将产时间占用未连接队列，导致正常的 SYN 请求因为队列满而被丢弃，从而引起网 络堵塞甚至系统瘫痪。SYN 攻击时一种典型的 DDOS 攻击，检测 SYN 攻击的方式非常简单，即当 Server 上有大量半连接状态且源 IP 地址是随机的，则可以断定遭到 SYN 攻击了，使用如下命令可以让之现行：

```
# netstat -nap | grep SYN_RECV
```
