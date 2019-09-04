# 块设备 IO 栈

介绍块设备的 IO 栈之前，我们先来了解一下块 IO 栈的几个基本概念：

- bio: bio 是通用块层 IO 请求的数据结构，表示上层提交的 IO 请求，一个 bio 包含多个 page，这些 page 必须对应磁盘上一段连续的空间。由于文件在磁盘上并不连续存放，文件 IO 提交到块设备之前，极有可能被拆成多个 bio 结构。

- request: 表示块设备驱动层 I/O 请求，经由 I/O 调度层转换后的 I/O 请求，将会发到块设备驱动层进行处理。

- request_queue: 维护块设备驱动层 I/O 请求的队列，所有的 request 都插入到该队列，每个磁盘设备都只有一个 queue（多个分区也只有一个）。

这 3 个结构的关系如下图示：一个 request_queue 中包含多个 request，每个 request 可能包含多个 bio，请求的合并就是根据各种原则将多个 bio 加入到同一个 requesst 中。

![块 IO 结构](https://s2.ax1x.com/2019/09/04/nEIaDS.png)

# 请求处理方式

![块 IO 处理流程](https://s2.ax1x.com/2019/09/04/nEI0EQ.png)

如图所示是块设备的 I/O 栈，其中的红色文字表示关键 I/O 路径的函数。对于 I/O 的读写流程，逻辑比较复杂，这里以写流程简单描述如下：

- 用户调用系统调用 write 写一个文件，会调到 sys_write 函数；

- 经过 VFS 虚拟文件系统层，调用 vfs_write， 如果是缓存写方式，则写入 page cache，然后就返回，后续就是刷脏页的流程；如果是 Direct I/O 的方式，就会走到 do_blockdev_direct_IO 的流程；

- 如果操作的设备是逻辑设备如 LVM，MDRAID 设备等，会进入到对应内核模块的处理函数里进行一些处理，否则就直接构造 bio 请求，调用 submit_bio 往具体的块设备下发请求，submit_bio 函数通过 generic_make_request 转发 bio，generic_make_request 是一个循环，其通过每个块设备下注册的 q->make_request_fn 函数与块设备进行交互；

- 请求下发到底层的块设备上，调用块设备请求处理函数`__make_request`进行处理，在这个函数中就会调用 blk_queue_bio，这个函数就是合并 bio 到 request 中，也就是 I/O 调度器的具体实现：如果几个 bio 要读写的区域是连续的，就合并到一个 request；否则就创建一个新的 request，把自己挂到这个 request 下。合并 bio 请求也是有限度的，如果合并后的请求超过阈值（在/sys/block/xxx/queue/max_sectors_kb 里设置）,就不能再合并成一个 request 了，而会新分配一个 request；

- 接下来的 I/O 操作就与具体的物理设备有关了，交由相应的块设备驱动程序进行处理，这里以 scsi 设备为例说明，queue 队列的处理函数`q->request_fn` 对应的 scsi 驱动的就是 `scsi_request_fn` 函数，将请求构造成 scsi 指令下发到 scsi 设备进行处理，处理完成后就会依次调用各层的回调函数进行完成状态的一些处理，最后返回给上层用户。

# request-based 和 bio-based

在块设备的 I/O 处理流程中，会涉及到两种不同的处理方式：

- request-based：这种处理方式下，会进行 bio 合并到 request（即 I/O 调度合并）的流程，最后才把请求下发到物理设备。目前使用的物理盘都是 request-based 的设备；

- bio-based：在逻辑设备自己定义的 request 处理函数 make_request_fn 里进行处理，然后调用 generic_make_request 下发到底层设备。ramdisk 设备、大部分 Device Mapper 设备、virtio-blk 都是 bio-based；

下图从 Device Mapper 的角度来说明 request-based 和 bio-based 处理流程的区别。

![](https://s2.ax1x.com/2019/09/04/nVkBa4.png)

一个需要注意的地方是，Device mapper 目前只有 multipath 插件是 request-based 的，其他的如 linear，strip 都是 bio-based，所以如果是 linear DM 设备上创建的一个文件系统，对这个文件系统里的文件进行读写，采用缓存 I/O 时，即使刷脏页时是连续的请求，在 DM 设备上也不会进行合并，只会到底层的设备（如/dev/sdb）上才进行合并。
