1. 基础包(包括工具包和安全包)

包括工具和安全包。其中，hdfs.util 包含了一些 HDFS 实现需要的辅助数据结构；hdfs.security.token.block 和 hdfs.security.token.delegation 结合 Hadoop 的安全框架，提供了安全访问 HDFS 的机制。

hdfs.util (一些 HDFS 实现需要的辅助数据结构)

AtomicFileOutputStream.java---- 继承实现类：原子文件输出流类；DataTransferThrottler.java---- 独立内存类：数据传送时的调节参数配置表；这个类是线程安全的，能够被多个线程所共享；LightWeightGSet.java---- 继承实现类：一个低内存占用的实现类；

hdfs.security.token.block (安全访问 HDFS 机制)

BlockKey.java---- 继承实现类：Key 用于生成和校验块的令牌；BlockTokenIdentifier.java---- 继承实现类：块令牌的标识符；BlockTokenSecretManager.java---- 继承实现类：块令牌管理类；BlockTokenSecretManager 能够被实例化为两种模式，主模式和从模式。主节点能够生成新的块 key，并导出块 key 到从节点。从节点只能导入并且使用从主节点接收的块 key。主机和从机都可以生成和验证块令牌。BlockTokenSelector.java---- 继承实现类：为 HDFS 的块令牌选择；ExportedBlockKeys.java---- 继承实现类：传递块 key 对象；InvalidBlockTokenException.java---- 继承实现类：访问令牌验证失败；

hdfs.security.token.delegation (安全访问 HDFS 机制)

DelegationTokenIdentifier.java---- 继承实现类：特定针对 HDFS 的代表令牌的标识符；DelegationTokenRenewer.java---- 继承实现类：这是一个守护进程，实现等待下一个文件系统的接续；DelegationTokenSecretManager.java---- 继承实现类：这个类实现了 HDFS 特定授权的令牌的管理；这个管理类实现了生成并接受每一个令牌的密码；DelegationTokenSelector.java---- 继承实现类：专门针对 HDFS 的令牌；

2.HDFS 实体实现包

这是代码分析的重点，包含 8 个包：

hdfs.server.common 包含了一些名字节点和数据节点共享的功能，如系统升级、存储空间信息等。

hdfs.protocol 和 hdfs.server.protocol 提供了 HDFS 各个实体间通过 IPC 交互的接口的定义和实现。

hdfs.server.namenode 、 hdfs.server.datanode 和 hdfs 分别包含了名字节点、数据节点和客户端的实现。上述代码是 HDFS 代码分析的重点。

hdfs.server.namenode.metrics 和 hdfs.server.datanode.metrics 实现了名字节点和数据节点上度量数据的收集功能。度量数据包括名字节点进程和数据节点进程上事件的计数，例如数据节点上就可以收集到写入字节数、被复制的块的数量等信息。

hdfs.server.common (一些名字节点和数据节点共享的功能)

GenerationStamp.java---- 继承实现类：生成时间戳的功能及读写访问类；HdfsConstants.java---- 接口类：一些 HDFS 内部的常数；Hdfs 常量字段及取值的定义；InconsistentFSStateException.java---- 继承实现类：不一致的文件系统状态异常；文件状态检查出错提示信息；IncorrectVersionException.java---- 继承实现类：不正确的版本异常；版本不正确检查时提示信息；Storage.java---- 继承实现类：存储信息文件；本地存储信息存储在一个单独的文件版本中；它包含了节点类型、存储布局版本、命名空间 ID 以及文件系统状态创建时间；内存中以扩展表记录方式记录当前 namenode 索引信息及状态信息(文件是否在打开)；StorageInfo.java---- 独立内存类：存储信息的通用类；内存中文件索引信息基本表；基本作用是保存地储上的文件系统元信息；Upgradeable.java---- 接口类：分布式升级对象的通用接口；对象升级接口方法集定义；UpgradeManager.java---- 独立内存类、抽象：通用升级管理；UpgradeObject.java---- 继承实现类：抽象升级对象；包含常用接口方法的实现；可升级对象的接口方法实现；UpgradeObjectCollection.java---- 独立内存类：可升级对象的集合容器实现；升级对象在使用前应该先进行注册，才能使用；UpgradeStatusReport.java---- 继承实现类：系统升级基类；升级过程状态信息表定义；Util.java---- 独立内存类：获取当前系统时间；

hdfs.protocol ( HDFS 各个实体间通过 IPC 交互的接口)

AlreadyBeingCreatedException.java---- 继承实现类：文件已经建立异常；Block.java---- 继承实现类：数据块在 HDFS 中的抽象；Bolck 内存基本块结构定义与读写访问；这个类是大量和数据块相关的类的基础，在客户端接口上，这样的类有 LocatedBlock、LocateBlocks 和 BlockLocalPathInfo。BlockListAsLongs.java---- 独立内存类：这个类提供了一个访问块列表的接口；Bolck 块的索引构成数组；该类的作用，就是将块数组 blcokArray 中的数据，“ 原封不动 ” 的转换成一个 long 类型的数组 blockList；BlockLocalPathInfo.java---- 继承实现类：应用于 ClientDatanodeProtocol 接口中，用于 HDFS 读文件的数据节点本地读优化。当客户端发现它和它要读取的数据块正好位于同一台主机上的时候，它可以不通过数据节点读数据块，而是直接读取本地文件，以获取数据块的内容。这大大降低了对应数据节点的负载。ClientDatanodeProtocol.java---- 接口类：客户端与数据节点间的接口。用于客户端和数据节点进行交互，这个接口用得比较少，客户端和数据节点间的主要交互是通过流接口进行读 / 写文件数据的操作。错误发生时，客户端需要数据节点配合进行恢复，或当客户端进行本地文件读优化时，需要通过 IPC 接口获取一些信息。ClientProtocol.java---- 接口类：客户端与 namenode 之间的接口；是 HDFS 客户访问文件系统的入口；客户端通过这个接口访问名字节点，操作文件或目录的元数据信息；读写文件也必须先访问名字节点，接下来再和数据节点进行交互，操作文件数据；另外，从名字节点能获取分布式文件系统的一些整体运行状态信息，也是通过这个接口进行的；用于访问 NameNode。它包含了文件角度上的 HDFS 功能。和 GFS 一样，HDFS 不提供 POSIX 形式的接口，而是使用了一个私有接口。一般来说，程序员通过 org.apache.hadoop.fs.FileSystem 来和 HDFS 打交道，不需要直接使用该接口。DatanodeID.java---- 继承实现类：用于在 HDFS 集群中确定一个数据节点；DatanodeInfo.java---- 继承实现类：继承自 DatanodeID，在 DatanodeID 的基础上，提供了数据节点上的一些度量信息；Datanode 状态信息结构定义及访问读写类；DataTransferProtocol.java---- 接口类：客户端与数据节点之间应用流协议传输数据的实现；DirectoryListing.java---- 继承实现类：用于一次返回一个目录下的多个文件 / 子目录的属性；DSQuotaExceededException.java---- 继承实现类：磁盘空间超出配额异常类；FSConstants.java---- 接口类：一些有用的常量；HdfsFileStatus.java---- 继承实现类：保存了 HDFS 文件 / 目录的属性；LayoutVersion.java---- 独立内存类：这个类跟踪了 HDFS 布局版本中的改变信息；LocatedBlock.java---- 继承实现类：已经确认了存储位置的数据块；可以用于一次定位多个数据块；LocatedBlocks.java---- 继承实现类：块的位置和文件长度集合；一组数据块及文件长度说明信息表的定义及读写；NSQuotaExceededException.java---- 继承实现类：命名空间超出配额异常类；QuotaExceededException.java---- 继承实现类：超出配额异常；UnregisteredDatanodeException.java---- 继承实现类：未注册的数据节点异常；

hdfs.server.protocol ( HDFS 各个实体间接口的实现)

BalancerBandwidthCommand.java---- 继承实现类：管理员通过调用 "dfsadmin -setBalanacerBandwidth newbandwidth" 实现动态的调整 balancer 的带宽参数；BlockCommand.java---- 继承实现类：BlockCommand 类实现的是数据节点控制下的块的指令；数据块命令定义及实现类；BlockMetaDataInfo.java---- 继承实现类：一个块的元数据信息；数据块的元数据信息定义及实现；BlockRecoveryInfo.java---- 继承实现类：块恢复操作信息；BlocksWithLocations.java---- 继承实现类：BlockLocations 序列的实现类；带位置的块信息的读写；DatanodeCommand.java---- 抽象类：数据节点命令；数据节点基本信息定义及实现；DatanodeProtocol.java---- 接口类：服务器间接口：数据节点与名字节点间的接口。在 HDFS 的主从体系结构中，数据节点作为从节点，不断的通过这个接口主节点名字节点报告一些信息，同步信息到名字节点；同时，该接口的一些方法，方法的返回值会带回名字节点指令，根据这些指令，数据节点或移动、或删除，或恢复本地磁盘上的数据块，或者执行其他的操作。DatanodeRegistration.java---- 继承实现类：DatanodeRegistration 类包含名字节点识别和验证数据节点的所有信息；数据节点的注册信息读写方法定义及实现；DisallowedDatanodeException.java---- 继承实现类：不允许的数据节点异常；InterDatanodeProtocol.java---- 接口类：服务器间的接口：数据节点与数据节点间的接口。数据节点通过这个接口，和其他数据节点进行通信，恢复数据块，保证数据的一致性。KeyUpdateCommand.java---- 继承实现类：key 升级命令；NamenodeProtocol.java---- 接口类：服务期间接口：第二名字节点、HDFS 均衡器与名字节点间的接口。第二名字节点会不停的获取名字节点上某一个时间点的命名空间镜像和镜像的变化日志，然后会合并得到一个新的镜像，并将该结果发送回名字节点，在这个过程中，名字节点会通过这个接口，配合第二名字节点完成元数据的合并。该接口也为 HDFS 均衡器 balancer 的正常工作提供一些信息。NamespaceInfo.java---- 继承实现类：NamespaceInfo 类实现了返回名字节点针对数据节点的握手；UpgradeCommand.java---- 继承实现类：这是一个通用的分布式升级命令类；升级数据块命名实现；

hdfs.server.namenode (名字节点的实现)

BlockPlacementPolicy.java---- 抽象类：这个接口用于选择放置块副本的目标磁盘的所需的数目；BlockPlacementPolicyDefault.java---- 继承实现类：这个类实现了选择放置块副本的目标磁盘的所需的数目；BlockPlacementPolicyWithNodeGroup.java---- 继承实现类：这个类实现了在 node-group 层上选择放置块副本的目标磁盘的所需的数目；BlockInfo.java---- 独立内存类：这个类维护了块到它元数据的映射；CancelDelegationTokenServlet.java---- 继承实现类：取消代表令牌服务；CheckpointSignature.java---- 继承实现类：检查点签名类；存贮信息的签名信息表定义；ContentSummaryServlet.java---- 继承实现类：文件校验服务；CorruptReplicasMap.java---- 独立内存类：存储文件系统中所有损坏的块的信息；DatanodeDescriptor.java---- 继承实现类：DatanodeDescriptor 类跟踪并统计了给定的数据节点上的信息，比如可用存储空间，上次更新时间等等；数据节点的状态信息定义及实现；DecommissionManager.java---- 独立内存类：管理节点的解除；DfsServlet.java---- 抽象类：DFS 服务的基类；Web 方式操作 DFS 的代理接口；EditLogInputStream.java---- 抽象类：一个通用的抽象类用来支持从持久型存储读取编辑日志数据；读取日志数据的类方法定义及实现；EditLogOutputStream.java---- 继承实现类：一个通用的抽象类用来支持从持久型存储记录编辑日志数据；写日志数据的类方法定义及实现；FileChecksumServlets.java---- 独立内存类：文件校验服务；文件较验 web 操作命令的代理实现；FileDataServlet.java---- 继承实现类：文件数据 web 操作命令的代理实现；FsckServlet.java---- 继承实现类：名字节点上 fsck 的 web 服务；文件系统检查 web 操作命令的代理实现；FSClusterStats.java---- 接口类：这个接口用于检索集群相关的统计；FSDirectory.java---- 继承实现类：类 FSDirectory 实现了存储文件系统的目录状态；文件目录结构的定义及实现；FSEditLog.java---- 独立内存类：FSEditLog 类实现了维护命名空间改动的日志记录；文件系统日志表的定义；FSImage.java---- 继承实现类：FSImage 实现对命名空间的编辑进行检查点操作和日志记录操作；文件系统的目录、文件、数据的索引及关系信息定义；FSInodeInfo.java---- 接口类：文件系统相关信息；FSNamesystem.java---- 继承实现类：FSNamesystem 类实现了为数据节点进行实际的记账工作；为数据节点命名的信息结构定义；FSPermissionChecker.java---- 独立内存类：实现用于检测文件系统权限的类；GetDelegationTokenServlet.java---- 继承实现类：获取委托令牌服务；GetImageServlet.java---- 继承实现类：这个类用于在命名系统中检索文件；通常用于第二名字节点来检索镜像以及为周期性的检测点进行编辑文件；Host2NodesMap.java---- 独立内存类：主机到节点的映射；INode.java---- 继承实现类：这个抽象类包含了文件和目录索引节点的通用字段；节点基础信息结构定义；INodeDirectory.java---- 继承实现类：表示目录的索引节点的类；INodeDirectoryWithQuota.java---- 继承实现类：有配额限制的目录索引节点类；INodeFile.java---- 继承实现类：目录索引节点文件；文件节点信息结构定义；INodeFileUnderConstruction.java---- 继承实现类：建立目录索引节点文件；在创建之下的文件节点信息结构定义；JspHelper.java---- 独立内存类：JSP 实现辅助类；LeaseExpiredException.java---- 继承实现类：创建的文件已过期异常；LeaseManager.java---- 独立内存类：LeaseManager 实现了写文件的租赁管理；这个类还提供了租赁恢复的有用的静态方法；契约信息结构定义及实现；ListPathsServlet.java---- 继承实现类：获取一个文件系统的元信息；MetaRecoveryContext.java---- 独立内存类：正在进行的名字节点恢复进程的上下文数据；NameCache.java---- 独立内存类：缓存经常使用的名称以便再用；NameNode.java---- 继承实现类：名字节点功能管理和实现类；名称节点的核心服务器类；NamenodeFsck.java---- 独立内存类：这个类提供了 DFS 卷基本的检测；名称节点的系统检测类；NameNodeMXBean.java---- 接口类：这个类是名字节点信息的 JMX 管理接口；NotReplicatedYetException.java---- 继承实现类：文件还未赋值异常类；PendingReplicationBlocks.java---- 独立内存类：这个类 PendingReplicationBlocks 实现了所有快复制的记录；正在复制数据块的信息表定义；PermissionChecker.java---- 独立内存类：这个类实现了执行权限检查操作；权限检查表结构定义及实现；RenewDelegationTokenServlet.java---- 继承实现类：续订令牌服务；SafeModeException.java---- 继承实现类：当名字节点处于安全模式的时候抛出这个异常；客户端不能够修改名字空间直到安全模式关闭；SecondaryNameNode.java---- 继承实现类：第二名字节点功能的管理和实现类；SerialNumberManager.java---- 独立内存类：为用户和组管理名称到序列号的映射；StreamFile.java---- 继承实现类：流文件类的实现；TransferFsImage.java---- 继承实现类：这个类实现了从名字节点获取一个指定的文件的功能；通过 http 方式来获取文件的镜像信息；UnderReplicatedBlocks.java---- 继承实现类：复制块的类的实现；复制完成后的块信息表定义；UnsupportedActionException.java---- 继承实现类：操作不支持的异常；UpgradeManagerNamenode.java---- 继承实现类：名字节点升级的管理；UpgradeObjectNamenode.java---- 抽象类：名字节点对象更新类；数据节点的更新运行在单独的线程上；升级名称节点的对象信息；

hdfs.server.datanode (数据节点的实现)

BlockAlreadyExistsException.java---- 继承实现类：目标块已经存在异常；BlockMetadataHeader.java---- 独立内存类：数据块头部结构定义及实现；BlockReceiver.java---- 继承实现类：这个类实现了接收一个块并写到自己的磁盘的功能，同时也可以复制它到另外一个磁盘；数据块接收容器信息结构及实现写入到盘中；接收一个数据块，并写到本地的磁盘，同时可能会拷贝到其他的节点上；BlockSender.java---- 继承实现类：从磁盘读取块，并发送它到接收的目的地；从磁盘中读数据块并发送到相应接收者；BlockTransferThrottler.java---- 独立内存类：调节数据块的传输；块传送时的调节参数配置表；DataBlockScanner.java---- 继承实现类：数据块的扫描工具实现；DataBlockScanner 拥有它单独的线程，能定时地从目前 DataNode 管理的数据块文件进行校验；其实最重要的方法就是 verifyBlock；DataBlockScanner 其他的辅助方法用于对 DataBlockScanner 管理的数据块文件信息进行增加 / 删除，排序操作；DataNode.java---- 继承实现类：数据节点的功能的管理和实现；数据块的核心管理器；DatanodeBlockInfo.java---- 独立内存类：这个类用于数据节点保持从数据块到它的元数据的映射的功能；建立块文件和其属于哪个 FSVolume 之间的映射关系；DataNodeMXBean.java---- 接口类：数据节点信息的 JMX 管理接口的实现；DataStorage.java---- 继承实现类：数据存储信息文件；DataXceiver.java---- 继承实现类：用于处理输入 / 输出数据流的线程；DataXceiverServer.java---- 继承实现类：用于接收和发送数据块的服务；FSDataset.java---- 继承实现类：FSDataset 类实现管理数据块集合的功能；FSDatasetAsyncDiskService.java---- 独立内存类：这个类实现了每个卷上多个线程池的容器，所以我们能够轻松地调度异步磁盘操作；为每个数据块目录创建一个线程池，并用作为一个线程组，池的最小值为 1，最大值为 4；当前版本，只有 delete 操作会将任务经过线程池调度来进行异步处理，读写操作都是调文件操作同步去执行的；FSDatasetInterface.java---- 接口类：这是一个接口，这个接口实现了一个数据节点存储块的底层存储；FSDatasetInterface 是 DataNode 对底局存储的抽象；SecureDataNodeStarter.java---- 继承实现类：这个类实现在一个安全的集群中启动一个数据节点，在启动前需要获得特权资源，并把它们递交给数据节点；UpgradeManagerDatanode.java---- 继承实现类：这个类实现了数据节点的升级管理；UpgradeObjectDatanode.java---- 抽象类：这个类是数据节点升级对象的基类；数据节点升级运行在一个单独的线程中；

hdfs (客户端的实现)

BlockReader.java---- 接口类：这个接口供本地和远程块读取共享；BlockReaderLocal.java---- 继承实现类：本地块读取；ByteRangeInputStream.java---- 抽象类：为了支持 HTTP 字节流，每次都需要建立一个针对 HTTP 服务的新的连接；ChecksumDistributedFileSystem.java---- 继承实现类：分布式文件系统检测；DFSClient.java---- 独立内存类：DFSClient 类能够连接到 Hadoop 文件系统并执行基本的文件操作；DFSConfigKeys.java---- 继承实现类：这个类包含了 HDFS 中使用的常数；DFSUtil.java---- 独立内存类：DFS 实用工具；DistributedFileSystem.java---- 继承实现类：DFS 系统的抽象文件系统实现；分布式文件系统功能的实现；HftpFileSystem.java---- 继承实现类：通过 HTTP 访问文件系统的协议的执行；采用 HTTP 协议来访问 HDFS 文件；HsftpFileSystem.java---- 继承实现类：通过 HTTPs 访问文件系统的协议的执行；采用 HTTPs 协议来访问 HDFS 文件；LeaseRenewer.java---- 独立内存类：更新租赁；

hdfs.server.namenode.metrics (名字节点上度量数据的收集功能)

FSNamesystemMBean.java---- 接口类：这个接口定义了获取一个名字节点的 FSNamesystem 状态的方法；取名称节点的文件状态信息；NameNodeInstrumentation.java---- 继承实现类：名字节点规范类；

hdfs.server.datanode.metrics (数据节点上度量数据的收集功能)

DataNodeInstrumentation.java---- 继承实现类：数据节点的一些规范；FSDatasetMBean.java---- 接口类：这个接口中定义了方法来获取一个数据节点的 FSDataset 的状态；数据集的职能定义；

3. 应用包

包括 hdfs.tools 和 hdfs.server.balancer，这两个包提供查询 HDFS 状态信息工具 dfsadmin、文件系统检查工具 fsck 和 HDFS 均衡器 balancer(通过 start-balancer.sh 启动)的实现。

hdfs.tools (查询 HDFS 状态信息工具 dfsadmin、文件系统检查工具 fsck 的实现)

DelegationTokenFetcher.java---- 独立内存类：这个类实现了从当前的名字节点获取 DelegationToken，并存储它到指定的文件当中；DFSAdmin.java---- 继承实现类：这个类实现了提供一些 DFS 访问管理；管理员命令的实现；DFSck.java---- 继承实现类：这个类实现了对 DFS 卷进行基本的检查；文件系统检查命令的实现；HDFSConcat.java---- 独立内存类：HDFS 串接；

hdfs.server.balancer ( HDFS 均衡器 balancer 的实现)

Balancer.java---- 继承实现类：负载均衡进程，各节点基于他来进行任务量的平衡；balance 是一个工具，用于当一些数据节点全部使用或者有新的节点加入集群的时候，来平衡 HDFS 集群上的磁盘空间使用率；

4.WebHDFS 相关包

包括 hdfs.web.resources、hdfs.server.namenode.metrics.web.resources 、 hdfs.server.datanode.web.resources 和 hdfs.web 共 4 个包。

WebHDFS 是 HDFS 1.0 中引入的新功能，它提供了一个完整的、通过 HTTP 访问 HDFS 的机制。对比只读的 hftp 文件系统，WebHDFS 提供了 HTTP 上读写 HDFS 的能力，并在此基础上实现了访问 HDFS 的 C 客户端和用户空间文件系统(FUSE )。
