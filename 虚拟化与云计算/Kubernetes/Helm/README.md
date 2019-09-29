# Helm

Helm 可以理解为 Kubernetes 的包管理工具，可以方便地发现、共享和使用为 Kubernetes 构建的应用，有点类似于 Ubuntu 的 APT 和 CentOS 中的 yum。Helm chart 是用来封装 Kubernetes 原生应用程序的 yaml 文件，可以在你部署应用的时候自定义应用程序的一些 metadata，便与应用程序的分发。

它包含几个基本概念：

- Chart：一个 Helm 包，其中包含了运行一个应用所需要的镜像、依赖和资源定义等，还可能包含 Kubernetes 集群中的服务定义，类似 Homebrew 中的 formula，APT 的 dpkg 或者 Yum 的 rpm 文件，

- Release: 在 Kubernetes 集群上运行的 Chart 的一个实例。在同一个集群上，一个 Chart 可以安装很多次。每次安装都会创建一个新的 release。例如一个 MySQL Chart，如果想在服务器上运行两个数据库，就可以把这个 Chart 安装两次。每次安装都会生成自己的 Release，会有自己的 Release 名称。

- Repository：用于发布和存储 Chart 的仓库。

# Helm 组件

Helm 采用客户端/服务器架构，有如下组件组成：

- Helm CLI 是 Helm 客户端，可以在本地执行。

- Tiller 是服务器端组件，在 Kubernetes 群集上运行，并管理 Kubernetes 应用程序的生命周期。

- Repository 是 Chart 仓库，Helm 客户端通过 HTTP 协议来访问仓库中 Chart 的索引文件和压缩包。
