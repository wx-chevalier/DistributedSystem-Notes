# 容器编排

Mesos 则更善于构建一个可靠的平台，用来运行多任务关键工作负载，包括 Docker 容器、遗留应用程序（如 Java）和分布式数据服务（如 Spark、Kafka、Cassandra、Elastic）。Mesos 采用两级调度的架构，开发人员可以很方便地结合公司的业务场景定制 Mesos Framework。

其实无论是 Kubernetes 还是 Mesos，它们都不是专门为了容器而开发的。Mesos 早于 Docker 出现，而 Kubernetes 的前身 Borg 更是早已出现，它们都是基于解除资源与应用程序本身的耦合限制而开发的。运行于容器中的应用，其轻量级的特性恰好能够与编排调度系统完美结合。

唯一为了 Docker 而生的编排系统是 Swarm，它由 Docker 所在的 Moby 公司出品，用于编排基于 Docker 的容器实例。不同于 Kubernetes 和 Mesos，Swarm 是面向 Docker 容器的，相较于 Kubernetes 面向云原生 PaaS 平台，以及 Mesos 面向“大数据+编排调度”平台，Swarm 显得功能单一。在容器技术本身已不是重点的今天，编排能力和生态规划均略逊一筹的 Swarm 已经跟不上前两者的脚步。
