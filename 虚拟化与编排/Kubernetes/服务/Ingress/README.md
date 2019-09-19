# Ingress

Ingress 是从 Kubernetes 集群外部访问集群内部服务的入口。Service 虽然解决了服务发现和负载均衡的问题，但它在使用上还是有一些限制，比如对外访问的时候，NodePort 类型需要在外部搭建额外的负载均衡，而 LoadBalancer 要求 Kubernetes 必须跑在支持的 Cloud Provider 上面。Ingress 就是为了解决这些限制而引入的新资源，主要用来将服务暴露到 Cluster 外面，并且可以自定义服务的访问策略。比如想要通过负载均衡器实现不同子域名到不同服务的访问：

```xml
foo.bar.com --|                 |-> foo.bar.com s1:80
              | 178.91.123.132  |
bar.foo.com --|                 |-> bar.foo.com s2:80
```

如上的需求我们可以定义为如下的 Ingress Controller：

```yml
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: test
spec:
  rules:
    - host: foo.bar.com
      http:
        paths:
          - backend:
              serviceName: s1
              servicePort: 80
    - host: bar.foo.com
      http:
        paths:
          - backend:
              serviceName: s2
              servicePort: 80
```

Ingress 本身并不会自动创建负载均衡器，集群中需要运行一个 Ingress Controller 来根据 Ingress 的定义来管理负载均衡器。

# 链接

- https://www.digitalocean.com/community/tutorials/how-to-set-up-an-nginx-ingress-with-cert-manager-on-digitalocean-kubernetes
