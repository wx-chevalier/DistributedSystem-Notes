## Snowflake

Twitter 的 Snowflake 算法产生的背景相当简单，为了满足 Twitter 每秒上万条消息的请求，每条消息都必须分配一条唯一的 id，这些 id 还需要一些大致的顺序(方便客户端排序)，并且在分布式系统中不同机器产生的 id 必须不同。把时间戳，工作机器 id，序列号组合在一起。

![](http://121.40.136.3/wp-content/uploads/2015/04/snowflake-64bit.jpg)

除了最高位 bit 标记为不可用以外，其余三组 bit 占位均可浮动，看具体的业务需求而定。默认情况下 41bit 的时间戳可以支持该算法使用到 2082 年，10bit 的工作机器 id 可以支持 1023 台机器，序列号支持 1 毫秒产生 4095 个自增序列 id。在 [关系型数据库理论 https://url.wx-coder.cn/DJNQn ](https://url.wx-coder.cn/DJNQn)一文中，我们也讨论了该算法的作用。

### 时间戳

这里时间戳的细度是毫秒级，具体代码如下，建议使用 64 位 linux 系统机器，因为有 vdso，gettimeofday()在用户态就可以完成操作，减少了进入内核态的损耗。

```
uint64_t generateStamp()
{
    timeval tv;
    gettimeofday(&tv, 0);
    return (uint64_t)tv.tv_sec * 1000 + (uint64_t)tv.tv_usec / 1000;
}

```

默认情况下有 41 个 bit 可以供使用，那么一共有 T(1llu << 41)毫秒供你使用分配，年份 = T / (3600 _ 24 _ 365 \* 1000) = 69.7 年。如果你只给时间戳分配 39 个 bit 使用，那么根据同样的算法最后年份 = 17.4 年。总体来说，是一个很高效很方便的 GUID 产生算法，一个 int64_t 字段就可以胜任，不像现在主流 128bit 的 GUID 算法，即使无法保证严格的 id 序列性，但是对于特定的业务，比如用做游戏服务器端的 GUID 产生会很方便。另外，在多线程的环境下，序列号使用 atomic 可以在代码实现上有效减少锁 的密度。

### 工作机器 ID

严格意义上来说这个 bit 段的使用可以是进程级，机器级的话你可以使用 MAC 地址来唯一标示工作机器，工作进程级可以使用 IP+Path 来区分工作进程。如果工作机器比较少，可以使用配置文件来设置这个 id 是一个不错的选择，如果机器过多配置文件的维护是一个灾难性的事情。
这里的解决方案是需要一个工作 id 分配的进程，可以使用自己编写一个简单进程来记录分配 id，或者利用 Mysql auto_increment 机制也可以达到效果。
![](http://121.40.136.3/wp-content/uploads/2015/04/snowflake-%E5%B7%A5%E4%BD%9Cid.jpg)
工作进程与工作 id 分配器只是在工作进程启动的时候交互一次，然后工作进程可以自行将分配的 id 数据落文件，下一次启动直接读取文件里的 id 使用。PS：这个工作机器 id 的 bit 段也可以进一步拆分，比如用前 5 个 bit 标记进程 id，后 5 个 bit 标记线程 id 之类。

### 序列号

序列号就是一系列的自增 id(多线程建议使用 atomic)，为了处理在同一毫秒内需要给多条消息分配 id，若同一毫秒把序列号用完了，则“等待至下一毫秒”。

```

uint64_t waitNextMs(uint64_t lastStamp)
{
    uint64_t cur = 0;
    do {
        cur = generateStamp();
    } while (cur <= lastStamp);
    return cur;
}

```
