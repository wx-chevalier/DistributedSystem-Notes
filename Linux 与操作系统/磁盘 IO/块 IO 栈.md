# 块设备 IO 栈

介绍块设备的 IO 栈之前，我们先来了解一下块 IO 栈的几个基本概念：

- bio: bio 是通用块层 IO 请求的数据结构，表示上层提交的 IO 请求，一个 bio 包含多个 page，这些 page 必须对应磁盘上一段连续的空间。由于文件在磁盘上并不连续存放，文件 IO 提交到块设备之前，极有可能被拆成多个 bio 结构。

- request: 表示块设备驱动层 I/O 请求，经由 I/O 调度层转换后的 I/O 请求，将会发到块设备驱动层进行处理。

- request_queue: 维护块设备驱动层 I/O 请求的队列，所有的 request 都插入到该队列，每个磁盘设备都只有一个 queue（多个分区也只有一个）。

这 3 个结构的关系如下图示：一个 request_queue 中包含多个 request，每个 request 可能包含多个 bio，请求的合并就是根据各种原则将多个 bio 加入到同一个 requesst 中。

![块 IO 结构](https://s2.ax1x.com/2019/09/04/nEIaDS.png)

# 请求处理方式

![](https://s2.ax1x.com/2019/09/04/nEI0EQ.png)
