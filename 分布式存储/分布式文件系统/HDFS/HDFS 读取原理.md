# Introduction

HDFS 开创性地设计出一套文件存储方式，即对文件分割后分别存放；HDFS 将要存储的大文件进行分割，分割后存放在既定的存储块(Block )中，并通过预先设定的优化处理，模式对存储的数据进行预处理，从而解决了大文件储存与计算的需求；一个 HDFS 集群包括两大部分，即 NameNode 与 DataNode。一般来说，一个集群中会有一个 NameNode 和多个 DataNode 共同工作；NameNode 是集群的主服务器，主要是用于对 HDFS 中所有的文件及内容数据进行维护，并不断读取记录集群中 DataNode 主机情况与工作状态，并通过读取与写入镜像日志文件的方式进行存储；DataNode 在 HDFS 集群中担任任务具体执行角色，是集群的工作节点。文件被分成若干个相同大小的数据块，分别存储在若干个 DataNode 上，DataNode 会定期向集群内 NameNode 发送自己的运行状态与存储内容，并根据 NameNode 发送的指令进行工作；NameNode 负责接受客户端发送过来的信息，然后将文件存储位置信息发送给提交请求的客户端，由客户端直接与 DataNode 进行联系，从而进行部分文件的运算与操作。Block 是 HDFS 的基本存储单元，默认大小是 64M；HDFS 还可以对已经存储的 Block 进行多副本备份，将每个 Block 至少复制到 3 个相互独立的硬件上，这样可以快速恢复损坏的数据；用户可以使用既定的 API 接口对 HDFS 中的文件进行操作；当客户端的读取操作发生错误的时候，客户端会向 NameNode 报告错误，并请求 NameNode 排除错误的 DataNode 后后重新根据距离排序，从而获得一个新的 DataNode 的读取路径。如果所有的 DataNode 都报告读取失败，那么整个任务就读取失败；对于写出操作过程中出现的问题，FSDataOutputStream 并不会立即关闭。客户端向 NameNode 报告错误信息，并直接向提供备份的 DataNode 中写入数据。备份 DataNode 被升级为首选 DataNode，并在其余 2 个 DataNode 中备份复制数据。NameNode 对错误的 DataNode 进行标记以便后续对其进行处理

# Quick Start

HDFS 基本命令 :

hadoop fs -cmd

cmd: 具体的操作，基本上与 UNIX 的命令行相同

args: 参数

HDFS 资源 URI 格式：

scheme://authority/path

scheme：协议名，file 或 hdfs

authority：namenode 主机名

path：路径

示例：hdfs://localhost:9000/user/chunk/test.txt

假设已经在 core-site.xml 里配置了 fs.default.name=hdfs://localhost:9000，则仅使用 /user/chunk/test.txt 即可。

hdfs 默认工作目录为 /user/$USER，$USER 是当前的登录用户名。

HDFS 命令示例：

hadoop fs -mkdir /user/trunk

hadoop fs -ls /user

hadoop fs -lsr /user ( 递归的 )

hadoop fs -put test.txt /user/trunk

hadoop fs -put test.txt . ( 复制到 hdfs 当前目录下，首先要创建当前目录 )

hadoop fs -get /user/trunk/test.txt . ( 复制到本地当前目录下 )

hadoop fs -cat /user/trunk/test.txt

hadoop fs -tail /user/trunk/test.txt ( 查看最后 1000 字节 )

hadoop fs -rm /user/trunk/test.txt

hadoop fs -help ls ( 查看 ls 命令的帮助文档 )

图中的 2：文件备份数量，因为采用了两台机器的全分布模式，所以此处为 2. 对于目录，使用 -。

在 put 的时候遇到问题：

## Configuration

## Read File

# 存储结构

## 行存储

HDFS 块内行存储的例子: ![](http://dl.iteye.com/upload/attachment/0083/5102/c5adc6f6-4a57-3994-b44c-2a943152bc58.png)

基于 Hadoop 系统行存储结构的优点在于快速数据加载和动态负载的高适应能力，这是因为行存储保证了相同记录的所有域都在同一个集群节点，即同一个 HDFS 块。不过，行存储的缺点也是显而易见的，例如它不能支持快速查询处理，因为当查询仅仅针对多列表中的少数几列时，它不能跳过不必要的列读取；此 外，由于混合着不同数据值的列，行存储不易获得一个极高的压缩比，即空间利用率不易大幅提高。

## 列存储

![](http://dl.iteye.com/upload/attachment/0083/5104/a432e6af-9a73-355c-ac77-b7c185da959c.jpg) 在 HDFS 上按照列组存储表格的例子。在这个例子中，列 A 和列 B 存储在同一列组，而列 C 和列 D 分别存储在单独的列组。查询时列存储能够避免读不必要的列，并且压缩一个列中的相似数据能够达到较高的压缩比。然而，由于元组重构的较高开销，它并不能提供基于 Hadoop 系统的快速查询处理。列存储不能保证同一 记录的所有域都存储在同一集群节点，行存储的例子中，记录的 4 个域存储在位于不同节点的 3 个 HDFS 块中。因此，记录的重构将导致通过集群节点网络的大 量数据传输。尽管预先分组后，多个列在一起能够减少开销，但是对于高度动态的负载模式，它并不具备很好的适应性。

# 数据读取

hdfs 读取数据流程图: ![](http://img.blog.csdn.net/20160525114335782) 1 、首先调用 FileSystem 对象的 open 方法，其实获取的是一个 DistributedFileSystem 的实例。2 、 DistributedFileSystem 通过 RPC( 远程过程调用 ) 获得文件的第一批 block 的 locations，同一 block 按照重复数会返回多个 locations，这些 locations 按照 hadoop 拓扑结构排序，距离客户端近的排在前面。3 、前两步会返回一个 FSDataInputStream 对象，该对象会被封装成 DFSInputStream 对象，DFSInputStream 可以方便的管理 datanode 和 namenode 数据流。客户端调用 read 方 法，DFSInputStream 就会找出离客户端最近的 datanode 并连接 datanode。4 、数据从 datanode 源源不断的流向客户端。5 、如果第一个 block 块的数据读完了，就会关闭指向第一个 block 块的 datanode 连接，接着读取下一个 block 块。这些操作对客户端来说是透明的，从客户端的角度来看只是读一个持续不断的流。6 、如果第一批 block 都读完了，DFSInputStream 就会去 namenode 拿下一批 blocks 的 location，然后继续读，如果所有的 block 块都读完，这时就会关闭掉所有的流。

# 数据写入

hdfs 写数据流程: ![](http://img.blog.csdn.net/20160525131839917) 1. 客户端通过调用 DistributedFileSystem 的 create 方法，创建一个新的文件。2.DistributedFileSystem 通过 RPC(远程过程调用)调用 NameNode，去创建一个没有 blocks 关联的新文件。创建前，NameNode 会做各种校验，比如文件是否存在，客户端有无权限去创建等。如果校验通过，NameNode 就会记录下新文件，否则就会抛出 IO 异常。3. 前两步结束后会返回 FSDataOutputStream 的对象，和读文件的时候相似，FSDataOutputStream 被封装成 DFSOutputStream，DFSOutputStream 可以协调 NameNode 和 DataNode。客户端开始写数据到 DFSOutputStream,DFSOutputStream 会把数据切成一个个小 packet，然后排成队列 data queue。4.DataStreamer 会去处理接受 data queue，它先问询 NameNode 这个新的 block 最适合存储的在哪几个 DataNode 里，比如重复数是 3，那么就找到 3 个最适合的 DataNode，把它们排成一个 pipeline。DataStreamer 把 packet 按队列输出到管道的第一个 DataNode 中，第一个 DataNode 又把 packet 输出到第二个 DataNode 中，以此类推。5.DFSOutputStream 还有一个队列叫 ack queue，也是由 packet 组成，等待 DataNode 的收到响应，当 pipeline 中的所有 DataNode 都表示已经收到的时候，这时 akc queue 才会把对应的 packet 包移除掉。6. 客户端完成写数据后，调用 close 方法关闭写入流。7.DataStreamer 把剩余的包都刷到 pipeline 里，然后等待 ack 信息，收到最后一个 ack 后，通知 DataNode 把文件标示为已完成。

![](http://img.blog.csdn.net/20160525133509937)

# NameNode HA

NameNode HA 架构如下: ![](http://img.blog.csdn.net/20160525134854724)

- Active NameNode 和 Standby NameNode：

两台 NameNode 形成互备，一台处于 Active 状态，为主 NameNode，另外一台处于 Standby 状态，为备 NameNode，只有主 NameNode 才能对外提供读写服务。

- 主备切换控制器 ZKFailoverController：

  ZKFailoverController 作为独立的进程运行，对 NameNode 的主备切换进行总体控制。ZKFailoverController 能及时检测到 NameNode 的健康状况，在主 NameNode 故障时借助 Zookeeper 实现自动的主备选举和切换。

- Zookeeper 集群：

  为主备切换控制器提供主备选举支持。

- 共享存储系统：

  共享存储系统是实现 NameNode 的高可用最为关键的部分，共享存储系统保存了 NameNode 在运行过程中所产生的 HDFS 的元数据。主 NameNode 和备用 NameNode 通过共享存储系统实现元数据同步。在进行主备切换的时候，新的主 NameNode 在确认元数据完全同步之后才能继续对外提供服务。
