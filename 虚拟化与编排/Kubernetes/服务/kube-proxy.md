# kube-proxy

当 service 有了 port 和 nodePort 之后，就可以对内/外提供服务。那么其具体是通过什么原理来实现的呢？奥妙就在 kube-proxy 在本地 node 上创建的 iptables 规则。

![kube-proxy 路由规则](https://i.postimg.cc/QtpTC39C/image.png)

kube-proxy 通过配置 DNAT 规则（从容器出来的访问，从本地主机出来的访问两方面），将到这个服务地址的访问映射到本地的 kube-proxy 端口（随机端口）。然后 kube-proxy 会监听在本地的对应端口，将到这个端口的访问给代理到远端真实的 Pod 地址上去。

# Chains

创建 Service 以后，kube-proxy 会自动在集群里的 Node 上创建以下两条规则：KUBE-PORTALS-CONTAINER、KUBE-PORTALS-HOST。如果是 NodePort 方式，还会额外生成两条：KUBE-NODEPORT-CONTAINER、KUBE-NODEPORT-HOST

## KUBE-PORTALS-CONTAINER

主要将由网络接口到来的通过服务集群入口 `<cluster ip>:port` 的请求重定向到本地 kube-proxy 端口（随机端口）的映射，即来自本地容器的服务访问请求。这种情况的网络包不可能来自外部网络，因为 cluster ip 是个 virtual ip，外部网络中不存在这样的路由将该数据包发送到本机；所以该请求只能来自本地容器，从本地容器出来的访问，服务访问请求是通过本地容器虚拟网卡输入到本地网络接口的。

## KUBE-NODEPORT-CONTAINER

主要将由网络接口到来的通过服务集群外部入口 `<node ip>:nodePort` 的请求重定向到本地 kube-proxy 端口（随机端口）的映射；即来自 K8s 集群外部网络的服务访问请求，可以来自本机容器，也可以来自其他 node 的容器，还可以来自其他 node 的进程。

# kube-proxy 反向代理

不管是通过集群内部服务入口 `<cluster ip>:port` 还是通过集群外部服务入口 `<node ip>:nodePort` 的请求都将重定向到本地 kube-proxy 端口（随机端口）的映射，然后将到这个 kube-proxy 端口的访问给代理到远端真实的 Pod 地址上去。

![](https://i.postimg.cc/y8mx0P6p/image.png)

![](https://i.postimg.cc/mrTDjHtL/image.png)
