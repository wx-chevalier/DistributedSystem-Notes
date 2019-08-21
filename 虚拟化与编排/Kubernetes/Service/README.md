# Service

Service 是对一组提供相同功能的 Pods 的抽象，并为它们提供一个统一的入口。借助 Service，应用可以方便的实现服务发现与负载均衡，并实现应用的零宕机升级。Kubernetes Service 能够支持 TCP 和 UDP 协议，默认 TCP 协议，其通过标签来选取服务后端，一般配合 Replication Controller 或者 Deployment 来保证后端容器的正常运行。这一组 Pod 能够被 Service 访问到，通常是通过 Label Selector 实现的。

譬如考虑一个图片处理 backend，它运行了 3 个副本。这些副本是可互换的：frontend 不需要关心它们调用了哪个 backend 副本。然而组成这一组 backend 程序的 Pod 实际上可能会发生变化，frontend 客户端不应该也没必要知道，而且也不需要跟踪这一组 backend 的状态。Service 定义的抽象能够解耦这种关联。

另外，也可以将已有的服务以 Service 的形式加入到 Kubernetes 集群中来，只需要在创建 Service 的时候不指定 Label selector，而是在 Service 创建好后手动为其添加 endpoint。对 Kubernetes 集群中的应用，Kubernetes 提供了简单的 Endpoints API，只要 Service 中的一组 Pod 发生变更，应用程序就会被更新。对非 Kubernetes 集群中的应用，Kubernetes 提供了基于 VIP 的网桥的方式访问 Service，再由 Service 重定向到 backend Pod。

# 服务发现与负载均衡

在 Kubernetes 中，我们可能会遇到许多不同的代理的概念，主要包含以下几类：

- kubectl 代理：在用户的桌面或 pod 中运行，代理从本地主机地址到 Kubernetes apiserver；客户端到代理将使用 HTTP，代理到 apiserver 使用 HTTPS，定位 apiserver，添加身份验证 header。

- apiserver 代理：内置于 apiserver 中，将集群外部的用户连接到集群 IP，否则这些 IP 可能无法访问。运行在 apiserver 进程中，客户端代理使用 HTTPS（也可配置为 http），代理将根据可用的信息决定使用 HTTP 或者 HTTPS 代理到目标。可用于访问节点、Pod 或服务，在访问服务时进行负载平衡。

- kube proxy：运行在每个节点上，代理 UDP 和 TCP，不能代理 HTTP。提供负载均衡，只能用来访问服务。

- 位于 apiserver 之前的 Proxy/Load-balancer：存在和实现因集群而异（例如 nginx），位于所有客户和一个或多个 apiserver 之间，如果有多个 apiserver，则充当负载均衡器。

- 外部服务上的云负载均衡器：由一些云提供商提供（例如 AWS ELB，Google Cloud Load Balancer），当 Kubernetes 服务类型为 LoadBalancer 时自动创建，只使用 UDP/TCP，具体实现因云提供商而异。

Kubernetes 在设计之初就充分考虑了针对容器的服务发现与负载均衡机制，提供了 Service 资源，并通过 kube-proxy 配合 Cloud Provider 来适应不同的应用场景。随着 kubernetes 用户的激增，用户场景的不断丰富，又产生了一些新的负载均衡机制。目前，kubernetes 中的负载均衡大致可以分为以下几种机制，每种机制都有其特定的应用场景：

- Service：直接用 Service 提供 cluster 内部的负载均衡，并借助 cloud provider 提供的 LB 提供外部访问

- Ingress Controller：还是用 Service 提供 cluster 内部的负载均衡，但是通过自定义 LB 提供外部访问

- Service Load Balancer：把 load balancer 直接跑在容器中，实现 Bare Metal 的 Service Load Balancer

- Custom Load Balancer：自定义负载均衡，并替代 kube-proxy，一般在物理部署 Kubernetes 时使用，方便接入公司已有的外部服务
