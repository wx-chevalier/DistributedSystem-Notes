# 状态监控（State Watch）

**原文**

https://martinfowler.com/articles/patterns-of-distributed-systems/state-watch.html

服务器上特定的值发生改变时，通知客户端。

**2021.1.19**

## 问题

客户端会对服务器上特定值的变化感兴趣。如果客户端要持续不断地轮询服务器，查看变化，它们就很难构建自己的逻辑。如果客户端与服务器之间打开许多连接，监控变化，服务器会不堪重负。

## 解决方案

让客户端将自己感兴趣的特定状态变化注册到服务器上。状态发生变化时，服务器会通知感兴趣的客户端。客户端同服务器之间维护了一个[单一 Socket 通道（Single Socket Channel）](single-socket-channel.md)。服务器通过这个通道发送状态变化通知。客户端可能会对多个值感兴趣，如果每个监控都维护一个连接的话，服务器将不堪重负。因此，客户端需要使用[请求管道（Request Pipeline）](request-pipeline.md)。

在[一致性内核（Consistent Core）](consistent-core.md)里，我们用了一个简单键值存储的例子，考虑一下这种场景：一个客户端对“某个特定键值对应值的改变，或是删除一个键值”感兴趣。实现包含了两个部分，客户端实现，服务器端实现。

### 客户端实现

客户端接收一个键值和一个函数，这个函数会在接收到服务器端监控事件时调用。客户端将函数对象存储起来，以备后续调用。然后，发送请求给服务器，注册这个监控。

```java
ConcurrentHashMap<String, Consumer<WatchEvent>> watches = new ConcurrentHashMap<>();

public void watch(String key, Consumer<WatchEvent> consumer) {
    watches.put(key, consumer);
    sendWatchRequest(key);
}

private void sendWatchRequest(String key) {
    requestSendingQueue.submit(new RequestOrResponse(RequestId.WatchRequest.getId(),
            JsonSerDes.serialize(new WatchRequest(key)),
            correlationId.getAndIncrement()));
}
```

如果连接上收到了监控事件，就会调用相应的消费者。

```java
this.pipelinedConnection = new PipelinedConnection(address, requestTimeoutMs, (r) -> {
    logger.info("Received response on the pipelined connection " + r);
    if (r.getRequestId() == RequestId.WatchRequest.getId()) {
        WatchEvent watchEvent = JsonSerDes.deserialize(r.getMessageBodyJson(), WatchEvent.class);
        Consumer<WatchEvent> watchEventConsumer = getConsumer(watchEvent.getKey());
        watchEventConsumer.accept(watchEvent);
        lastWatchedEventIndex = watchEvent.getIndex(); //capture last watched index, in case of connection failure.
    }
    completeRequestFutures(r);
});
```

### 服务端实现

服务端接收到监控的注册请求时，它会保持一个映射，也就是接收请求的管道连接同键值之间的映射。

```java
private Map<String, ClientConnection> watches = new HashMap<>();
private Map<ClientConnection, List<String>> connection2WatchKeys = new HashMap<>();

public void watch(String key, ClientConnection clientConnection) {
    logger.info("Setting watch for " + key);
    addWatch(key, clientConnection);
}

private synchronized void addWatch(String key, ClientConnection clientConnection) {
    mapWatchKey2Connection(key, clientConnection);
    watches.put(key, clientConnection);
}

private void mapWatchKey2Connection(String key, ClientConnection clientConnection) {
    List<String> keys = connection2WatchKeys.get(clientConnection);
    if (keys == null) {
        keys = new ArrayList<>();
        connection2WatchKeys.put(clientConnection, keys);
    }
    keys.add(key);
}
```

ClientConnection 封装了与客户端之间的 Socket 连接。其结构如下。无论是基于阻塞 IO 的服务器，还是基于非阻塞 IO 的服务器，其结构都是一样的。

```java
public interface ClientConnection {
    void write(RequestOrResponse response);
    void close();
}
```

一个连接上可以注册多个监控。因此，将连接同监控键值列表的映射存储起来是很重要的。当客户端关闭连接时，删除所有相关的监控是必要的，就像下面这样：

```java
public void close(ClientConnection connection) {
        removeWatches(connection);
    }
    private synchronized void removeWatches(ClientConnection clientConnection) {
        List<String> watchedKeys = connection2WatchKeys.remove(clientConnection);
        if (watchedKeys == null) {
            return;
        }
        for (String key : watchedKeys) {
            watches.remove(key);
        }
    }
```

当服务器发生了特定事件，比如，给一个键值设置了值，服务器就会构造一个相关的 WatchEvent，然后，通知给所有注册的客户端。

```java
private synchronized void notifyWatchers(SetValueCommand setValueCommand, Long entryId) {
    if (!hasWatchesFor(setValueCommand.getKey())) {
        return;
    }
    String watchedKey = setValueCommand.getKey();
    WatchEvent watchEvent = new WatchEvent(watchedKey,
                                setValueCommand.getValue(),
                                EventType.KEY_ADDED, entryId);
    notify(watchEvent, watchedKey);
}

private void notify(WatchEvent watchEvent, String watchedKey) {
    List<ClientConnection> watches = getAllWatchersFor(watchedKey);
    for (ClientConnection pipelinedClientConnection : watches) {
        try {
            String serializedEvent = JsonSerDes.serialize(watchEvent);
            getLogger().trace("Notifying watcher of event "
                    + watchEvent +
                    " from "
                    + server.getServerId());
            pipelinedClientConnection
                    .write(new RequestOrResponse(RequestId.WatchRequest.getId(),
                            serializedEvent));
        } catch (NetworkException e) {
            removeWatches(pipelinedClientConnection); //remove watch if network connection fails.
        }
    }
}
```

有一个需要注意的关键点是，监控相关的状态要能够并发访问，有的是来自客户端请求处理代码，有的是来自客户端连接处理代码来关闭连接。因此，所有访问监控状态的方法都需要用锁进行保护。

### 在层次结构存储中的监控

[一致性内核（Consistent Core）](consistent-core.md)大多支持有层次结构的存储。监控可以设置在父节点或是键值的前缀上。子节点的任何变化都会触发父节点上的监控集。对于每个事件而言，一致性内容都会遍历一下路径，检查父路径上是否设置了监控，给所有的监控发送事件。

```java
List<ClientConnection> getAllWatchersFor(String key) {
    List<ClientConnection> affectedWatches = new ArrayList<>();
    String[] paths = key.split("/");
    String currentPath = paths[0];
    addWatch(currentPath, affectedWatches);
    for (int i = 1; i < paths.length; i++) {
        currentPath = currentPath + "/" + paths[i];
        addWatch(currentPath, affectedWatches);
    }
    return affectedWatches;
}

private void addWatch(String currentPath, List<ClientConnection> affectedWatches) {
    ClientConnection clientConnection = watches.get(currentPath);
    if (clientConnection != null) {
        affectedWatches.add(clientConnection);
    }
}
```

这样就可以在键值前缀上设置一个监控，比如`servers`。任何用这个前缀创建出的键值，比如，`server/1`、`server/2` 都会触发这个监控。

因为待调用函数同键值前缀的映射要一起存储，对于客户端而言，有一点很重要，根据收到的事件，遍历层次结构，查找待调用的函数。一种替代方案是，将事件同事件触发的路径一起发送回去，这样一来，客户端就知道到发送过来的状态是由哪个监控引发的了。

### 处理连接失效

客户端和服务器间的连接随时可能失效。就某些用例而言，这是有问题的，因为客户端在其失联期间，可能会错过某些事件。比如说，集群控制器可能对是否有节点失效感兴趣，其表现方式就是一些键值移除的事件。客户端要把其接收到最后接收的事件告诉服务器。客户端在重新设置监控时，会发送其最后接收的事件号。服务器要把从这个事件号之后记录的所有事件都发送出来。

在[一致性内核（Consistent Core）](consistent-core.md)的客户端里，这可以与领导者重新建立连接时完成。

```java
private void connectToLeader(List<InetAddressAndPort> servers) {
    while (isDisconnected()) {
        logger.info("Trying to connect to next server");
        waitForPossibleLeaderElection();
        establishConnectionToLeader(servers);
    }
    setWatchesOnNewLeader();
}
private void setWatchesOnNewLeader() {
    for (String watchKey : watches.keySet()) {
        sendWatchResetRequest(watchKey);
    }
}
private void sendWatchResetRequest(String key) {
    pipelinedConnection.send(new RequestOrResponse(RequestId.SetWatchRequest.getId(),
            JsonSerDes.serialize(new SetWatchRequest(key, lastWatchedEventIndex)), correlationId.getAndIncrement()));
}
```
服务器会给发送的每个事件编号。比如，如果服务器是[一致性内核（Consistent Core）](consistent-core.md)，它会以严格的顺序存储所有的状态变化，每个变化都用日志索引来编号，这在[预写日志（Write-Ahead Log）](write-ahead-log.md)里已经讨论过了。这样一来，客户端要得到从特定索引开始的事件，就是可能实现的。

### 来自键值存储的派生事件

事件也可以通过查看键值存储的当前状态来生成，它还可以对发生的变化进行编号，将这个编号与每个值一起存储起来。

当客户端重新建立同服务器的连接，它可以再次设置监控，还要发送最后一次看到变化的编号。服务器可以将其与存储的值相比较，如果这个值大于客户端发送的值，它就要把事件重新发送给客户端。键值存储的派生事件可能有点尴尬，因为事件需要猜测。它可能会错过一些事件——比如，如果一个键值先创建后删除了——在客户端失联时，创建事件就会丢失了。

```java
private synchronized void eventsFromStoreState(String key, long stateChangesSince) {
    List<StoredValue> values = getValuesForKeyPrefix(key);
    for (StoredValue value : values) {
        if (values == null) {
            //the key was probably deleted send deleted event
            notify(new WatchEvent(key, EventType.KEY_DELETED), key);
        } else if (value.index > stateChangesSince) {
            //the key/value was created/updated after the last event client knows about
            notify(new WatchEvent(key, value.getValue(), EventType.KEY_ADDED, value.getIndex()), key);
        }
    }
}
```

[zookeeper](https://zookeeper.apache.org/) 使用的就是这种方式。在缺省情况下，Zookeeper 的监控是一次性触发器。一旦事件触发了，如果客户端还想收到进一步的事件，就要重新设置监控。在监控重新设置之前，有些事件有可能就丢失了，因此，客户端要确保它们读到的是最新的状态，这样，它们就不会丢失任何更新。

### 存储事件的历史

一个更容易的做法是，保存过去事件的历史，根据事件历史相应客户端。这种方式的问题在于，事件的历史需要有个限制，比如，1000 条事件。如果客户端长时间失联，它就会错过超过 1000 条事件窗口的事件。

一种简单的实现方式是使用 Google Guava 的 EvictingQueue，如下所示：

```java
public class EventHistory implements Logging {
    Queue<WatchEvent> events = EvictingQueue.create(1000);
    public void addEvent(WatchEvent e) {
        getLogger().info("Adding " + e);
        events.add(e);
    }
    public List<WatchEvent> getEvents(String key, Long stateChangesSince) {
        return this.events.stream()
                .filter(e -> e.getIndex() > stateChangesSince && e.getKey().equals(key))
                .collect(Collectors.toList());
    }
}
```

当客户端重新建立起连接，重新设置监控时，事件可以从历史中发送。

```java
private void sendEventsFromHistory(String key, long stateChangesSince) {
    List<WatchEvent> events = eventHistory.getEvents(key, stateChangesSince);
    for (WatchEvent event : events) {
        notify(event, event.getKey());
    }
}
```

### 使用多版本存储

为了追踪所有的变化，我们也可以使用多版本存储。它会保存每个键值的所有版本，这样，根据请求版本可以很容易地找出所有的变化。

[etcd](https://etcd.io/) 版本 3 之后的版本就使用了这种方式。

## 示例

[zookeeper](https://zookeeper.apache.org/) 能够在节点设置监控。像 [kafka](https://kafka.apache.org/) 这样的产品就用它存储分组成员信息，以及集群成员的失效检测。

[etcd](https://etcd.io/) 有一个监控的实现，[kubernetes](https://kubernetes.io/) 重度使用了它，用于其资源[监控](https://kubernetes.io/docs/reference/using-api/api-concepts/)的实现。
