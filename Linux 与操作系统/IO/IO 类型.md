# 数据存储与 IO

针对磁盘与针对网络的 IO。

- 单精度

![](https://coding.net/u/hoteam/p/Cache/git/raw/master/2017/8/1/v2-749cc641eb4d5dafd085e8c23f8826aa_r.png)

- 双精度

![](https://coding.net/u/hoteam/p/Cache/git/raw/master/2017/8/1/v2-48240f0e1e0dd33ec89100cbe2d30707_b.png)

在分区分配方案中，回收一个分区时有几种不同的邻接情况，在各种情况下应如何处理？ 答：有四种：上邻，下邻，上下相邻，上下不相邻。

(1) 回收分区的上邻分区是空闲的，需要将两个相邻的空闲区合并成一个更大的空闲区，然后修改空闲区表。

(2) 回收分区的下邻分区是空闲的，需要将两个相邻的空闲区合并成一个更大的空闲区，然后修改空闲区表。

(3) 回收分区的上、下邻分区都是空闲的(空闲区个数为 2)，需要将三个空闲区合并成一个更大的空闲区(空闲区个数为 1)，然后修改空闲区表、

(4) 回收分区的上、下邻分区都不是空闲的，则直接将空闲区记录在空闲区表中。

I/O 中断方式是以字节为单位，DMA 控制方式是以一个连续的数据块为单位，I/O 通道控制方式是 DMA 控制方式的发展，是以一组数据块为单位的，即可以连续读取多个数据块。 ( 1)程序直接访问方式跟循环检测 IO 方式，应该是一个意思吧，是最古老的方式。CPU 和 IO 串行，每读一个字节(或字)， CPU 都需要不断检测状态寄存 器的 busy 标志，当 busy=1 时，表示 IO 还没完成；当 busy=0 时，表示 IO 完成。此时读取一个字的过程才结束，接着读取下一个字。( 2)中断控制方式：循环检测先进些，IO 设备和 CPU 可以并行工作，只有在开始 IO 和结束 IO 时，才需要 CPU。但每次只能读取一个字。( 3)DMA 方式：Direct Memory Access ，直接存储器访问，比中断先进的地方是每次可以读取一个块，而不是一个字。( 4)通道方式：比 DMA 先进的地方是，每次可以处理多个块，而不只是一个块。

所以在[哪种方式在读取磁盘上多个顺序数据块时的效率最高](http://www.nowcoder.com/test/question/done?tid=4893778&qid=44781#summary) 这个题目中我们应该选择通道方式。

# I/O 读写的类型

大体上讲，I/O 的类型可以分为：读 / 写 I/O、大 / 小块 I/O、连续 / 随机 I/O, 顺序 / 并发 I/O。在这几种类型中，我们主要讨论一下：大 / 小块 I/O、连续 / 随机 I/O, 顺序 / 并发 I/O。

大 / 小块 I/O
这个数值指的是控制器指令中给出的连续读出扇区数目的多少。如果数目较多，如 64，128 等，我们可以认为是大块 I/O；反之，如果很小，比如 4，8，我们就会认为是小块 I/O，实际上，在大块和小块 I/O 之间，没有明确的界限。

连续 / 随机 I/O
连续 I/O 指的是本次 I/O 给出的初始扇区地址和上一次 I/O 的结束扇区地址是完全连续或者相隔不多的。反之，如果相差很大，则算作一次随机 I/O

连续 I/O 比随机 I/O 效率高的原因是：在做连续 I/O 的时候，磁头几乎不用换道，或者换道的时间很短；而对于随机 I/O，如果这个 I/O 很多的话，会导致磁头不停地换道，造成效率的极大降低。

顺序 / 并发 I/O
从概念上讲，并发 I/O 就是指向一块磁盘发出一条 I/O 指令后，不必等待它回应，接着向另外一块磁盘发 I/O 指令。对于具有条带性的 RAID（LUN），对其进行的 I/O 操作是并发的，例如：raid 0+1(1+0),raid5 等。反之则为顺序 I/O。

Not really. epoll only makes sense for file descriptors which would normally exhibit blocking behavior on read/write, like pipes and sockets. Normal file descriptors will always either return a result or end-of-file more or less immediately, so epoll wouldn't do anything useful for them.
