# 一致性哈希

当我们在做数据库分库分表或者是分布式缓存时，不可避免的都会遇到一个问题：如何将数据均匀的分散到各个节点中，并且尽量的在加减节点时能使受影响的数据最少。

最经典的方式就是 hash 取模了，即将传入的 Key 按照 `index=hash(key)%N` 这样来计算出需要存放的节点。其中 hash 函数是一个将字符串转换为正整数的哈希映射方法，N 就是节点的数量。这样可以满足数据的均匀分配，但是这个算法的容错性和扩展性都较差。比如增加或删除了一个节点时，所有的 Key 都需要重新计算，显然这样成本较高，为此需要一个算法满足分布均匀同时也要有良好的容错性和拓展性。

# 环构建

一致 Hash 算法是将所有的哈希值构成了一个环，其范围在 0~2^32-1。之后将各个节点散列到这个环上，可以用节点的 IP、hostname 这样的唯一性字段作为 Key 进行 hash 计算取值。客户端根据自身的某些数据 hash 之后也定位到这个环中，通过顺时针找到离他最近的一个节点，也就是这次路由的服务节点。

当节点发生故障下线，或者增加新的节点时候，依然根据顺时针方向，k2 和 k3 保持不变，只有 k1 被重新映射到了 N3。这样就很好的保证了容错性，当一个节点宕机时只会影响到少少部分的数据。

# 虚拟节点

考虑到服务节点的个数以及 hash 算法的问题导致环中的数据分布不均匀时引入了虚拟节点。

# Java 中一致性哈希算法的实现

我们可以选择用某个有序数组或者 TreeMap 来模拟一致性哈希中的环：

- 初始化一个长度为 N 的数组。
- 将服务节点通过 hash 算法得到的正整数，同时将节点自身的数据（hashcode、ip、端口等）存放在这里。
- 完成节点存放后将整个数组进行排序（排序算法有多种）。
- 客户端获取路由节点时，将自身进行 hash 也得到一个正整数；
- 遍历这个数组直到找到一个数据大于等于当前客户端的 hash 值，就将当前节点作为该客户端所路由的节点。
- 如果没有发现比客户端大的数据就返回第一个节点（满足环的特性）。

只使用了 TreeMap 的一些 API：

- 写入数据候，TreeMap 可以保证 key 的自然排序。
- tailMap 可以获取比当前 key 大的部分数据。
- 当这个方法有数据返回时取第一个就是顺时针中的第一个节点了。
- 如果没有返回那就直接取整个 Map 的第一个节点，同样也实现了环形结构。

# Go 中一致性哈希算法的实现与应用

```go
func (m *Map) Add(nodes ...string) {
    for _, n := range nodes {
        for i := 0; i < m.replicas; i++ {
            hash := int(m.hash([]byte(strconv.Itoa(i) + " " + n)))
            m.nodes = append(m.nodes, hash)
            m.hashMap[hash] = n
        }
    }
    sort.Ints(m.nodes)
}
```

```go
func (m *Map) Get(key string) string {
    hash := int(m.hash([]byte(key)))
    idx := sort.Search(len(m.keys),
        func(i int) bool { return m.keys[i] >= hash }
    )
    if idx == len(m.keys) {
        idx = 0
    }
    return m.hashMap[m.keys[idx]]
}
```

# 链接

- https://mp.weixin.qq.com/s?__biz=MzIyMzgyODkxMQ==&mid=2247484108&idx=1&sn=87df95335ca97aa5d44475fc2c8d1e4b&scene=21#wechat_redirect
- http://blog.carlosgaldino.com/consistent-hashing.html

* http://blog.carlosgaldino.com/consistent-hashing.html

- https://blog.csdn.net/ydyang1126/article/details/70313981
