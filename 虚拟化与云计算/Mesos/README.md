# Mesos

> Mesos is a meta, framework scheduler rather than an application scheduler like YARN.

> [mesos-docker-tutorial-how-to-build-your-own-framework](https://www.voxxed.com/blog/2014/12/mesos-docker-tutorial-how-to-build-your-own-framework/)

从各个常用的组件的协调角度，我们可以来看如下这张图：

![](http://img2.tuicool.com/IvQZni.png)

而从 Mesos 本身的功能模型，即 Master 与 Slave 的模型之间的关系的角度来看的话，可以得到如下模型：

![](http://i.stack.imgur.com/Gqhrx.jpg)

![](http://img1.tuicool.com/YbiArmi.jpg)

Slave 是运行在物理或虚拟服务器上的 Mesos 守护进程，是 Mesos 集群的一部分。Framework 由调度器(Scheduler)应用程序和任务执行器(Executor)组成，被注册到 Mesos 以使用 Mesos 集群中的资源。

- Slave 1 向 Master 汇报其空闲资源：4 个 CPU、4GB 内存。然后，Master 触发分配策略模块，得到的反馈是 Framework 1 要请求全部可用资源。
- Master 向 Framework 1 发送资源邀约，描述了 Slave 1 上的可用资源。
- Framework 的调度器(Scheduler)响应 Master，需要在 Slave 上运行两个任务，第一个任务分配<2 CPUs, 1 GB RAM>资源，第二个任务分配<1 CPUs, 2 GB RAM>资源。
- 最后，Master 向 Slave 下发任务，分配适当的资源给 Framework 的任务执行器(Executor),接下来由执行器启动这两个任务(如图中虚线框所示)。 此时，还有 1 个 CPU 和 1GB 的 RAM 尚未分配，因此分配模块可以将这些资源供给 Framework 2。

## Advantage

- 效率 – 这是最显而易见的好处，也是 Mesos 社区和 Mesosphere 经常津津乐道的。 ![](http://img2.tuicool.com/Bz67Zzm.jpg)

上图来自 Mesosphere 网站，描绘出 Mesos 为效率带来的好处。如今，在大多数数据中心中，服务器的静态分区是常态，即使使用最新的应用程序，如 Hadoop。这时常令人担忧的是，当不同的应用程序使用相同的节点时，调度相互冲突，可用资源互相争抢。静态分区本质上是低效的，因为经常会面临，其中一个分区已经资源耗尽，而另一个分区的资源却没有得到充分利用，而且没有什么简单的方法能跨分区集群重新分配资源。使用 Mesos 资源管理器仲裁不同的调度器，我们将进入动态分区/弹性共享的模式，所有应用程序都可以使用节点的公共池，安全地、最大化地利用资源。 一个经常被引用的例子是 Slave 节点通常运行 Hadoop 作业，在 Slave 空闲阶段，动态分配给他们运行批处理作业，反之亦然。 值得一提的是，这其中的某些环节可以通过虚拟化技术，如 VMware vSphere 的 [分布式资源调度(DRS)](http://wordpress.redirectingat.com/?id=725X1342&site=varchitectthoughts.wordpress.com&xs=1&isjs=1&url=http%3A%2F%2Fwww.vmware.com%2Fproducts%2Fvsphere%2Ffeatures%2Fdrs-dpm&xguid=1d9204bd07663e5f9ea0dd30373503c1&xuuid=2fd1c0d399d172da1ffd0c4fecbff774&xsessid=e67353eeda490b34a26e2fb37a2d7517&xcreo=0&xed=0&sref=http%3A%2F%2Fcloudarchitectmusings.com%2F2015%2F03%2F26%2Fdigging-deeper-into-apache-mesos%2F&xtz=-480) 来完成。 然而，Mesos 具有更精细的粒度，因为 Mesos 在应用层而不是机器层分配资源，通过容器而不是整个虚拟机(VM)分配任务。 前者能够为每个应用程序的特殊需求做考量，应用程序的调度器知道最有效地利用资源; 后者能够更好地“装箱”，运行一个任务，没有必要实例化一整个虚拟机，其所需的进程和二进制文件足矣。

- 敏捷 – 与效率和利用率密切相关，这实际上是我认为最重要的好处。 往往，效率解决的是“如何花最少的钱最大化数据中心的资源”，而敏捷解决的是“如何快速用上手头的资源。” 正如我和我的同事 [Tyler Britten](https://twitter.com/vmtyler) 经常指出，IT 的存在是帮助企业赚钱和省钱的；那么如何通过技术帮助我们迅速创收，是我们要达到的重要指标。 这意味着要确保关键应用程序不能耗尽所需资源，因为我们无法为应用提供足够的基础设施，特别是在数据中心的其他地方都的资源是收费情况下。

- 可扩展性 – 为可扩展而设计，这是我真心欣赏 Mesos 架构的地方。 这一重要属性使数据可以指数级增长、分布式应用可以水平扩展。 我们的发展已经远远超出了使用巨大的整体调度器或者限定群集节点数量为 64 的时代，足矣承载新形式的应用扩张。

  Mesos 可扩展设计的关键之处是采用两级调度架构。 使用 Framework 代理任务的实际调度，Master 可以用非常轻量级的代码实现，更易于扩展集群发展的规模。 因为 Master 不必知道所支持的每种类型的应用程序背后复杂的调度逻辑。 此外，由于 Master 不必为每个任务做调度，因此不会成为容量的性能瓶颈，而这在为每个任务或者虚拟机做调度的整体调度器中经常发生。

  ![](http://img2.tuicool.com/jiIVrq.jpg)

* 模块化 – 对我来说，预测任何开源技术的健康发展，很大程度上取决于围绕该项目的生态系统。 我认为 Mesos 项目前景很好，因为其设计具有包容性，可以将功能插件化，比如分配策略、隔离机制和 Framework。将容器技术，比如 Docker 和 Rocket 插件化的好处是显而易见。但是我想在此强调的是围绕 Framework 建设的生态系统。将任务调度委托给 Framework 应用程序，以及采用插件架构，通过 Mesos 这样的设计，社区创造了能够让 Mesos 问鼎数据中心资源管理的生态系统。因为每接入一种新的 Framework，Master 无需为此编码，Slave 模块可以复用，使得在 Mesos 所支持的宽泛领域中，业务迅速增长。相反，开发者可以专注于他们的应用和 Framework 的选择。 当前而且还在不断地增长着的 Mesos Framework 列表参见 [此处](http://mesos.apache.org/documentation/latest/mesos-frameworks/) 以及下图: ![](http://img2.tuicool.com/eaqyEr.png)

### 资源分配

为了实现在同一组 Slave 节点集合上运行多任务这一目标，Mesos 使用了隔离模块， 该模块使用了一些应用和进程隔离机制来运行这些任务。 不足为奇的是，虽然可以使用虚拟机隔离实现隔离模块，但是 Mesos 当前模块支持的是容器隔离。 Mesos 早在 2009 年就用上了 Linux 的容器技术，如 cgroups 和 Solaris Zone，时至今日这些仍然是默认的。 然而，Mesos 社区增加了 Docker 作为运行任务的隔离机制。 不管使用哪种隔离模块，为运行特定应用程序的任务，都需要将执行器全部打包，并在已经为该任务分配资源的 Slave 服务器上启动。 当任务执行完毕后，容器会被“销毁”，资源会被释放，以便可以执行其他任务。

我们来更深入地研究一下资源邀约和分配策略，因为这对 Mesos 管理跨多个 Framework 和应用的资源，是不可或缺的。 我们前面提到资源邀约的概念，即由 Master 向注册其上的 Framework 发送资源邀约。 每次资源邀约包含一份 Slave 节点上可用的 CPU、RAM 等资源的列表。 Master 提供这些资源给它的 Framework，是基于分配策略的。分配策略对所有的 Framework 普遍适用，同时适用于特定的 Framework。 Framework 可以拒绝资源邀约，如果它不满足要求，若此，资源邀约随即可以发给其他 Framework。 由 Mesos 管理的应用程序通常运行短周期的任务，因此这样可以快速释放资源，缓解 Framework 的资源饥饿； Slave 定期向 Master 报告其可用资源，以便 Master 能够不断产生新的资源邀约。 另外，还可以使用诸如此类的技术， 每个 Fraamework 过滤不满足要求的资源邀约、Master 主动废除给定周期内一直没有被接受的邀约。

分配策略有助于 Mesos Master 判断是否应该把当前可用资源提供给特定的 Framework，以及应该提供多少资源。 关于 Mesos 中使用资源分配以及可插拔的分配模块，实现非常细粒度的资源共享，会单独写一篇文章。 言归正传，Mesos 实现了公平共享和严格优先级(这两个概念我会在资源分配那篇讲)分配模块， 确保大部分用例的最佳资源共享。已经实现的新分配模块可以处理大部分之外的用例。
