# 容器技术与资源隔离

Linux cgroups 为一种名为 Linux 容器（LXC）的技术铺平了道路。 LXC 实际上是我们今天所知的第一个实现容器的主要实现，利用 cgroup 和命名空间隔离来创建具有独立进程和网络空间的虚拟环境。从某种意义上说，这允许独立和隔离的用户空间。  容器的概念直接来自 LXC。 事实上，早期版本的 Docker 直接构建在 LXC 之上。

简单的讲就是，Linux namespace 允许用户在独立进程之间隔离 CPU 等资源。进程的访问权限及可见性仅限于其所在的 Namespaces 。因此，用户无需担心在一个 Namespace 内运行的进程与在另一个 Namespace 内运行的进程冲突。甚至可以同一台机器上的不同容器中运行具有相同 PID 的进程。同样的，两个不同容器中的应用程序可以使用相同的端口。

![](https://tva1.sinaimg.cn/large/007rAy9hgy1g2zdhwngx6j30u00m0wgg.jpg)

与虚拟机相比，容器更轻量且速度更快，因为它利用了 Linux 底层操作系统在隔离的环境中运行。虚拟机的 hypervisor 创建了一个非常牢固的边界，以防止应用程序突破它，而容器的边界不那么强大。另一个区别是，由于 Namespace 和 Cgroups 功能仅在 Linux 上可用，因此容器无法在其他操作系统上运行；Docker 实际上使用了一个技巧，并在非 Linux 操作系统上安装 Linux 虚拟机，然后在虚拟机内运行容器。

虽然大多数 IT 行业正在采用基于容器的基础架构（云原生解决方案），但必须了解该技术的局限性。 传统容器（如 Docker，Linux Containers（LXC）和 Rocket（rkt））并不是真正的沙箱，因为它们共享主机操作系统内核。 它们具有资源效率，但攻击面和破坏的潜在影响仍然很大，特别是在多租户云环境中，共同定位属于不同客户的容器。

当主机操作系统为每个容器创建虚拟化用户空间时，问题的根源是容器之间的弱分离。一直致力于设计真正的沙盒容器的研究和开发。 大多数解决方案重新构建容器之间的边界以加强隔离。譬如来自 IBM，Google，Amazon 和 OpenStack 的四个独特项目，这些项目使用不同的技术来实现相同的目标，为容器创建更强的隔离。

- IBM Nabla 在 Unikernels 之上构建容器
- Google gVisor 创建了一个用于运行容器的专用客户机内核
- Amazon Firecracker 是一个用于沙盒应用程序的极轻量级管理程序
- OpenStack 将容器放置在针对容器编排平台优化的专用 VM 中

# 容器技术

# 链接

- https://unit42.paloaltonetworks.com/making-containers-more-isolated-an-overview-of-sandboxed-container-technologies
