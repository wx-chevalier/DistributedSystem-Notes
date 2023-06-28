# 单一 Socket 通道（Single Socket Channel）

**原文**

https://martinfowler.com/articles/patterns-of-distributed-systems/single-socket-channel.html

通过使用单一的 TCP 连接，维护发送给服务器请求的顺序。

**2020.8.19**

## 问题

使用[领导者和追随者（Leader and Followers）](leader-and-followers.md)时，我们需要确保在领导者和各个追随者之间的消息保持有序，如果有消息丢失，需要重试机制。我们需要做到这一点，还要保证保持新连接的成本足够低，开启新连接才不会增加系统的延迟。

## 解决方案

幸运的是，已经长期广泛使用的 [TCP](https://en.wikipedia.org/wiki/Transmission_Control_Protocol) 机制已经提供了所有这些必要的特征。因此，我们只要确保追随者与其领导者之间都是通过单一的 Socket 通道进行通信，就可以进行我们所需的通信。然后，追随者再对来自领导者的更新进行序列化，将其送入[单一更新队列（Singular Update Queue）](singular-update-queue.md)。

![单一 Socket 通道](https://ngte-superbed.oss-cn-beijing.aliyuncs.com/book/patterns-of-distributed-systems/single-socket-channel.png)

<center>图1：单一 Socket 通道</center>

节点一旦打开连接，就不会关闭，持续从中读取新的请求。节点为每个连接准备一个专用的线程去读取写入请求。如果使用的是[非阻塞 IO](<https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)>)，那就不需要为每个连接准备一个线程。

下面是一个基于简单线程的实现：

```java
class SocketHandlerThread…
  @Override
  public void run() {
      isRunning = true;
      try {
          //Continues to read/write to the socket connection till it is closed.
          while (isRunning) {
              handleRequest();
          }
      } catch (Exception e) {
          getLogger().debug(e);
          closeClient(this);
      }
  }

  private void handleRequest() {
      RequestOrResponse request = clientConnection.readRequest();
      RequestId requestId = RequestId.valueOf(request.getRequestId());
      server.accept(new Message<>(request, requestId, clientConnection));
  }

  public void closeConnection() {
      clientConnection.close();
  }
```

节点读取请求，将它们提交到[单一更新队列（Singular Update Queue）](singular-update-queue.md)中等待处理。一旦节点处理了写入的请求，它就将应答写回到 socket。

无论节点什么时候需要建立通信，它都会打开单一 Socket 连接，与对方通信的所有请求都会使用这个连接。

```java
class SingleSocketChannel…
  public class SingleSocketChannel implements Closeable {
      final InetAddressAndPort address;
      final int heartbeatIntervalMs;
      private Socket clientSocket;
      private final OutputStream socketOutputStream;
      private final InputStream inputStream;
  
      public SingleSocketChannel(InetAddressAndPort address, int heartbeatIntervalMs) throws IOException {
          this.address = address;
          this.heartbeatIntervalMs = heartbeatIntervalMs;
          clientSocket = new Socket();
          clientSocket.connect(new InetSocketAddress(address.getAddress(), address.getPort()), heartbeatIntervalMs);
          clientSocket.setSoTimeout(heartbeatIntervalMs * 10); //set socket read timeout to be more than heartbeat.
          socketOutputStream = clientSocket.getOutputStream();
          inputStream = clientSocket.getInputStream();
      }
  
      public synchronized RequestOrResponse blockingSend(RequestOrResponse request) throws IOException {
          writeRequest(request);
          byte[] responseBytes = readResponse();
          return deserialize(responseBytes);
      }
  
      private void writeRequest(RequestOrResponse request) throws IOException {
          var dataStream = new DataOutputStream(socketOutputStream);
          byte[] messageBytes = serialize(request);
          dataStream.writeInt(messageBytes.length);
          dataStream.write(messageBytes);
      }
```

有一点很重要，就是连接要有超时时间，这样就不会在出错的时候，造成永久阻塞了。我们使用[心跳（HeartBeat）](heartbeat.md)周期性地在 Socket 通道上发送请求，以便保活。超时时间通常都是多个心跳的间隔，这样，网络的往返时间以及可能的一些延迟就不会造成问题了。比方说，将连接超时时间设置成心跳间隔的 10 倍也是合理的。

```java
class SocketListener…
private void setReadTimeout(Socket clientSocket) throws SocketException {
clientSocket.setSoTimeout(config.getHeartBeatIntervalMs() * 10);
}
```

通过单一通道发送请求，可能会带来一个问题，也就是[队首阻塞（Head-of-line blocking，HOL）](https://en.wikipedia.org/wiki/Head-of-line_blocking)问题。为了避免这个问题，我们可以使用[请求管道（Request Pipeline）](request-pipeline.md)。

## 示例

[Zookeeper](https://zookeeper.apache.org/doc/r3.4.13/zookeeperInternals.html) 使用了单一 Socket 通道，每个追随者一个线程，处理所有的通信。

[Kafka](https://kafka.apache.org/protocol) 在追随者和领导者分区之间使用了单一 Socket 通道，进行消息复制。

[Raft](https://raft.github.io/) 共识算法的参考实现，[LogCabin](https://github.com/logcabin/logcabin) 使用单一 Socket 通道，在 领导者和追随者之间进行通信。
