# 部署方式的变化

在没有 Kubernetes 之前，我们大概要做这么些操作才能交付这个 nginx 服务：

- 到三个 Linux VM 上面，分别把三个 nginx 进程起好。这里可能还需要关心 nginx 进程怎么起、启动命令是啥、配置怎么配。

- 到负载均衡管理页面，申请一个负载均衡，把 3 个 nignx 进程的 IP 填入。拿回负载均衡的 IP。

* 到 DNS 管理页面申请一个 DNS 记录，写入把拿到的负载均衡的 IP 写入 A 记录。

- 把这个 DNS 记录作为这个 nginx 服务的交付成果，交付给用户。

在没有 Kubernetes 之前，我们大概要做这么些操作才能交付这个 nginx 服务：

有了 Kubernetes 之后， 我们只需要写一个 nginx 如何部署的 “菜单”，然后提交这个“菜单”给 Kubernetes，我们就完成了部署。 “菜单” 是一个 yaml 文件(例子中文件名 nginx.yaml)，大概这个样子:

```yaml
apiVersion: apps/v1
kind:
Deployment
metadata:
  name: nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      app-name: my-nginx

template:
    metadata:
      labels:
        app-name: my-nginx
    spec:
      containers:
        - name: nginx
          image: nginx
---
apiVersion: v1
kind:
Service
metadata:
  name: nginx
spec:
  selector:
    app-name: my-nginx
  type: ClusterIP
  ports:
    - name: http
      port: 80
      protocol: TCP
      targetPort: 80
```
