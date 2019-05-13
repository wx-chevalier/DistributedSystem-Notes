# 资源隔离

简单的讲就是，Linux namespace 允许用户在独立进程之间隔离 CPU 等资源。进程的访问权限及可见性仅限于其所在的 Namespaces 。因此，用户无需担心在一个 Namespace 内运行的进程与在另一个 Namespace 内运行的进程冲突。甚至可以同一台机器上的不同容器中运行具有相同 PID 的进程。同样的，两个不同容器中的应用程序可以使用相同的端口。

![](https://tva1.sinaimg.cn/large/007rAy9hgy1g2zdhwngx6j30u00m0wgg.jpg)

与虚拟机相比，容器更轻量且速度更快，因为它利用了 Linux 底层操作系统在隔离的环境中运行。虚拟机的 hypervisor 创建了一个非常牢固的边界，以防止应用程序突破它，而容器的边界不那么强大。另一个区别是，由于 Namespace 和 Cgroups 功能仅在 Linux 上可用，因此容器无法在其他操作系统上运行；Docker 实际上使用了一个技巧，并在非 Linux 操作系统上安装 Linux 虚拟机，然后在虚拟机内运行容器。