# Service 定义

一个 Service 在 Kubernetes 中是一个 REST 对象，和 Pod 类似。 像所有的 REST 对象一样， Service 定义可以基于 POST 方式，请求 Api Server 创建新的实例。典型的 Service 定义方式如下：

```yml
kind: Service
apiVersion: v1
metadata:
  name: hostname-service
spec:
  # Expose the service on a static port on each node
  # so that we can access the service from outside the cluster
  type: NodePort

  # When the node receives a request on the static port (30163)
  # "select pods with the label 'app' set to 'echo-hostname'"
  # and forward the request to one of them
  selector:
    app: echo-hostname

  ports:
    # Three types of ports for a service
    # nodePort - a static port assigned on each the node
    # port - port exposed internally in the cluster
    # targetPort - the container port to send requests to
    - nodePort: 30163
      port: 8080
      targetPort: 80
```

# 服务暴露

一个 Pod 只是一个运行服务的实例，随时可能在一个节点上停止，在另一个节点以一个新的 IP 启动一个新的 Pod，因此不能以确定的 IP 和端口号提供服务。要稳定地提供服务需要服务发现和负载均衡能力。服务发现完成的工作，是针对客户端访问的服务，找到对应的的后端服务实例。在 K8 集群中，客户端需要访问的服务就是 Service 对象。每个 Service 会对应一个集群内部有效的虚拟 IP，集群内部通过虚拟 IP 访问一个服务。Service 有三种类型：

- ClusterIP：默认类型，自动分配一个仅 Cluster 内部可以访问的虚拟 IP。

- NodePort：在 ClusterIP 基础上为 Service 在每台机器上绑定一个端口，这样就可以通过 `<NodeIP>:NodePort` 来访问该服务。

- LoadBalancer：在 NodePort 的基础上，借助 Cloud Provider 创建一个外部的负载均衡器，并将请求转发到 `<NodeIP>:NodePort`。

这里需要对于 Port 的定义进行简要说明：

- port 表示 Service 暴露在 Cluster IP 上的端口，`<cluster ip>:port` 是提供给集群内部客户访问 Service 的入口。

- nodePort 是 K8s 提供给集群外部客户访问 Service 入口的一种方式（另一种方式是 LoadBalancer），所以，`<nodeIP>:nodePort` 是提供给集群外部客户访问 Service 的入口。

- targetPort 是 Pod 上的端口，从 port 和 nodePort 上到来的数据最终经过 kube-proxy 流入到后端 Pod 的 targetPort 上进入容器。

总的来说，port 和 nodePort 都是 Service 的端口，前者暴露给集群内客户访问服务，后者暴露给集群外客户访问服务。从这两个端口到来的数据都需要经过反向代理 kube-proxy 流入后端 Pod 的 targetPod，从而到达 Pod 上的容器内。默认情况下，targetPort 将被设置为与 port 字段相同的值；targetPort 可以是一个字符串，引用了 backend Pod 的一个端口的名称。但是，实际指派给该端口名称的端口号，在每个 backend Pod 中可能并不相同。对于部署和设计 Service，这种方式会提供更大的灵活性；例如，可以在 backend 软件下一个版本中，修改 Pod 暴露的端口，并不会中断客户端的调用。

## 服务发现与负载均衡

在 Kubernetes 中，我们可能会遇到许多不同的代理的概念，主要包含以下几类：

- kubectl 代理：在用户的桌面或 pod 中运行，代理从本地主机地址到 Kubernetes apiserver；客户端到代理将使用 HTTP，代理到 apiserver 使用 HTTPS，定位 apiserver，添加身份验证 header。

- apiserver 代理：内置于 apiserver 中，将集群外部的用户连接到集群 IP，否则这些 IP 可能无法访问。运行在 apiserver 进程中，客户端代理使用 HTTPS（也可配置为 http），代理将根据可用的信息决定使用 HTTP 或者 HTTPS 代理到目标。可用于访问节点、Pod 或服务，在访问服务时进行负载平衡。

- kube proxy：运行在每个节点上，代理 UDP 和 TCP，不能代理 HTTP。提供负载均衡，只能用来访问服务。

- 位于 apiserver 之前的 Proxy/Load-balancer：存在和实现因集群而异（例如 nginx），位于所有客户和一个或多个 apiserver 之间，如果有多个 apiserver，则充当负载均衡器。

- 外部服务上的云负载均衡器：由一些云提供商提供（例如 AWS ELB，Google Cloud Load Balancer），当 Kubernetes 服务类型为 LoadBalancer 时自动创建，只使用 UDP/TCP，具体实现因云提供商而异。

Kubernetes 在设计之初就充分考虑了针对容器的服务发现与负载均衡机制，提供了 Service 资源，并通过 kube-proxy 配合 Cloud Provider 来适应不同的应用场景。随着 kubernetes 用户的激增，用户场景的不断丰富，又产生了一些新的负载均衡机制。目前，Kubernetes 中的负载均衡大致可以分为以下几种机制，每种机制都有其特定的应用场景：

- Service：直接用 Service 提供 Cluster 内部的负载均衡，并借助 Cloud Provider 提供的 LB 提供外部访问。

- Ingress Controller：还是用 Service 提供 Cluster 内部的负载均衡，但是通过自定义 LB 提供外部访问。

- Service Load Balancer：把 LB 直接跑在容器中，实现 Bare Metal 的 Service Load Balancer。

- Custom Load Balancer：自定义负载均衡，并替代 kube-proxy，一般在物理部署 Kubernetes 时使用，方便接入公司已有的外部服务。

# 其他类型的服务后端

Service 抽象了该如何访问 Kubernetes Pod，但也能够抽象其它类型的 backend，例如：

- 希望在生产环境中使用外部的数据库集群，但测试环境使用自己的数据库。
- 希望服务指向另一个 Namespace 中或其它集群中的服务。
- 正在将工作负载转移到 Kubernetes 集群，和运行在 Kubernetes 集群之外的 backend。

在任何这些场景中，都能够定义没有 selector 的 Service ：

```yml
kind: Service
apiVersion: v1
metadata:
  name: my-service
spec:
  ports:
    - protocol: TCP
      port: 80
      targetPort: 9376
```

由于这个 Service 没有 selector，就不会创建相关的 Endpoints 对象。可以手动将 Service 映射到指定的 Endpoints：

```yml
kind: Endpoints
apiVersion: v1
metadata:
  name: my-service
subsets:
  - addresses:
      - ip: 1.2.3.4
    ports:
      - port: 9376
```

注意，Endpoint IP 地址不能是 loopback（127.0.0.0/8）、 link-local（169.254.0.0/16）、或者 link-local 多播（224.0.0.0/24）。访问没有 selector 的 Service，与有 selector 的 Service 的原理相同。请求将被路由到用户定义的 Endpoint（该示例中为 1.2.3.4:9376）。ExternalName Service 是 Service 的特例，它没有 selector，也没有定义任何的端口和 Endpoint。 相反地，对于运行在集群外部的服务，它通过返回该外部服务的别名这种方式来提供服务。

```yml
kind: Service
apiVersion: v1
metadata:
  name: my-service
  namespace: prod
spec:
  type: ExternalName
  externalName: my.database.example.com
```

当查询主机 my-service.prod.svc.CLUSTER 时，集群的 DNS 服务将返回一个值为 my.database.example.com 的 CNAME 记录。 访问这个服务的工作方式与其它的相同，唯一不同的是重定向发生在 DNS 层，而且不会进行代理或转发。 如果后续决定要将数据库迁移到 Kubernetes 集群中，可以启动对应的 Pod，增加合适的 Selector 或 Endpoint，修改 Service 的 type。
