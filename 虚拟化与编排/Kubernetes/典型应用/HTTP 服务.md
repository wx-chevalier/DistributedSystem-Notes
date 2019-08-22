# 应用部署

# Deployment & Service

在 [K8s Boilerplates](https://github.com/wx-chevalier/Backend-Boilerplates/tree/master/K8s) 中我们定义了简单的 Nginx 的部署与服务，分别用于集群构建与对外的服务暴露：

```yml
# nginx-deployment-service.yaml
---
apiVersion: apps/v1 # for versions before 1.9.0 use apps/v1beta2
kind: Deployment
metadata:
  name: nginx
spec:
  strategy:
    type: Recreate
  selector:
    matchLabels:
      app: nginx
  replicas: 3 # tells deployment to run 1 pods matching the template
  template: # create pods using pod definition in this template
    metadata:
      labels:
        app: nginx
    spec:
      containers:
        - name: nginx
          image: nginx
          ports:
            - containerPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: nginx
  namespace: default
  labels:
    app: nginx
spec:
  externalTrafficPolicy: Local
  ports:
    - name: http
      port: 80
  selector:
    app: nginx
  type: NodePort
```

```sh
$ kubectl create -f https://raw.githubusercontent.com/wx-chevalier/Backend-Boilerplates/master/K8s/Base/nginx-deployment-service.yaml

$ kubectl get pod

NAME                                             READY   STATUS    RESTARTS   AGE
nginx-56db997f77-2q6qz                           1/1     Running   0          3m21s
nginx-56db997f77-fv2zs                           1/1     Running   0          3m21s
nginx-56db997f77-wx2q5                           1/1     Running   0          3m21s

$ kubectl get deployment

NAME                            READY   UP-TO-DATE   AVAILABLE   AGE
nginx                           3/3     3            3           3m36s

$ kubectl get svc

NAME                            TYPE           CLUSTER-IP      EXTERNAL-IP                              PORT(S)                      AGE
kubernetes                      ClusterIP      10.43.0.1       <none>                                   443/TCP                      21h
nginx                           NodePort       10.43.8.50      <none>                                   80:32356/TCP                 4m5s
```

![](https://i.postimg.cc/6qQZRXwh/image.png)

# Ingress

Ingress 是一种 Kubernetes 资源，也是将 Kubernetes 集群内服务暴露到外部的一种方式。ngress 只是一个统称，其由 Ingress 和 Ingress Controller 两部分组成。Ingress 用作将原来需要手动配置的规则抽象成一个 Ingress 对象，使用 YAML 格式的文件来创建和管理。Ingress Controller 用作通过与 Kubernetes API 交互，动态的去感知集群中 Ingress 规则变化。

目前可用的 Ingress Controller 类型有很多，比如：Nginx、HAProxy、Traefik 等，Nginx Ingress 使用 ConfigMap 来管理 Nginx 配置。

## Helm 安装 Ingress

```sh
$ helm install --name nginx-ingress --set "rbac.create=true,controller.service.externalIPs[0]=172.19.157.1,controller.service.externalIPs[1]=172.19.157.2,controller.service.e
xternalIPs[2]=172.19.157.3" stable/nginx-ingress

NAME:   nginx-ingress
LAST DEPLOYED: Tue Aug 20 14:50:13 2019
NAMESPACE: default
STATUS: DEPLOYED

RESOURCES:
==> v1/ConfigMap
NAME                      DATA  AGE
nginx-ingress-controller  1     0s

==> v1/Pod(related)
NAME                                            READY  STATUS             RESTARTS  AGE
nginx-ingress-controller-5f874f7bf4-nvsvv       0/1    ContainerCreating  0         0s
nginx-ingress-default-backend-6f598d9c4c-vj4v8  0/1    ContainerCreating  0         0s

==> v1/Service
NAME                           TYPE          CLUSTER-IP    EXTERNAL-IP                             PORT(S)                     AGE
nginx-ingress-controller       LoadBalancer  10.43.115.59  172.19.157.1,172.19.157.2,172.19.157.3  80:32122/TCP,443:32312/TCP  0s
nginx-ingress-default-backend  ClusterIP     10.43.8.65    <none>                                  80/TCP                      0s

==> v1/ServiceAccount
NAME           SECRETS  AGE
nginx-ingress  1        0s

==> v1beta1/ClusterRole
NAME           AGE
nginx-ingress  0s

==> v1beta1/ClusterRoleBinding
NAME           AGE
nginx-ingress  0s

==> v1beta1/Deployment
NAME                           READY  UP-TO-DATE  AVAILABLE  AGE
nginx-ingress-controller       0/1    1           0          0s
nginx-ingress-default-backend  0/1    1           0          0s

==> v1beta1/PodDisruptionBudget
NAME                           MIN AVAILABLE  MAX UNAVAILABLE  ALLOWED DISRUPTIONS  AGE
nginx-ingress-controller       1              N/A              0                    0s
nginx-ingress-default-backend  1              N/A              0                    0s
```

部署完成后我们可以看到 Kubernetes 服务中增加了 nginx-ingress-controller 和 nginx-ingress-default-backend 两个服务。nginx-ingress-controller 为 Ingress Controller，主要做为一个七层的负载均衡器来提供 HTTP 路由、粘性会话、SSL 终止、SSL 直通、TCP 和 UDP 负载平衡等功能。nginx-ingress-default-backend 为默认的后端，当集群外部的请求通过 Ingress 进入到集群内部时，如果无法负载到相应后端的 Service 上时，这种未知的请求将会被负载到这个默认的后端上。

```sh
$ kubectl get svc
NAME                            TYPE           CLUSTER-IP      EXTERNAL-IP                              PORT(S)                      AGE
kubernetes                      ClusterIP      10.43.0.1       <none>                                   443/TCP                      20h
nginx-ingress-controller        LoadBalancer   10.43.115.59    172.19.157.1,172.19.157.2,172.19.157.3   80:32122/TCP,443:32312/TCP   77m
nginx-ingress-default-backend   ClusterIP      10.43.8.65      <none>                                   80/TCP                       77m

$ kubectl --namespace default get services -o wide -w nginx-ingress-controller

NAME                       TYPE           CLUSTER-IP     EXTERNAL-IP                              PORT(S)                      AGE   SELECTOR
nginx-ingress-controller   LoadBalancer   10.43.115.59   172.19.157.1,172.19.157.2,172.19.157.3   80:32122/TCP,443:32312/TCP   77m   app=nginx-ingress,component=controller,release=nginx-ingress
```

由于我们采用了 ExternalIP 方式对外暴露服务， 所以 nginx-ingress-controller 会在三台节点宿主机上的 暴露 80/443 端口。我们可以在任意节点上进行访问，因为我们还没有在 Kubernetes 集群中创建 Ingress 资源，所以直接对 ExternalIP 的请求被负载到了 nginx-ingress-default-backend 上。nginx-ingress-default-backend 默认提供了两个 URL 进行访问，其中的 /healthz 用作健康检查返回 200，而 / 返回 404 错误。

```sh
$ curl 127.0.0.1/
# default backend - 404

$ curl 127.0.0.1/healthz/
# 返回的是 200
```

后续我们如果需要创建自身的 Ingress 配置，可以参考如下方式：

```yml
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  annotations:
    kubernetes.io/ingress.class: nginx
  name: example
  namespace: foo
spec:
  rules:
    - host: www.example.com
      http:
        paths:
          - backend:
              serviceName: exampleService
              servicePort: 80
            path: /
  # This section is only required if TLS is to be enabled for the Ingress
  tls:
    - hosts:
        - www.example.com
      secretName: example-tls
```

如果希望使用 TLS，那么需要创建包含证书与 Key 的 Secret：

```yml
apiVersion: v1
kind: Secret
metadata:
  name: example-tls
  namespace: foo
data:
  tls.crt: <base64 encoded cert>
  tls.key: <base64 encoded key>
type: kubernetes.io/tls
```

## WordPress

Helm 安装完毕后，我们来测试部署一个 WordPress 应用：

```sh
$ helm install --name wordpress-test --set "ingress.enabled=true,persistence.enabled=false,mariadb.persistence.enabled=false" stable/wordpress

NAME:  wordpress-test
...
```

这里我们使用 Ingress 负载均衡进行访问，可以通过如下方式访问到服务：

```sh
$ kubectl get ingress

NAME                             HOSTS             ADDRESS                                  PORTS   AGE
wordpress.local-wordpress-test   wordpress.local   172.19.157.1,172.19.157.2,172.19.157.3   80      59m

$ curl -I http://wordpress.local -x 127.0.0.1:80

HTTP/1.1 200 OK
Server: nginx/1.15.6
Date: Tue, 20 Aug 2019 07:55:21 GMT
Content-Type: text/html; charset=UTF-8
Connection: keep-alive
Vary: Accept-Encoding
X-Powered-By: PHP/7.0.27
Link: <http://wordpress.local/wp-json/>; rel="https://api.w.org/"
```

也可以根据 Charts 的说明，利用如下命令获得 WordPress 站点的管理员用户和密码：

```sh
echo Username: user
echo Password: $(kubectl get secret --namespace default wordpress-test-wordpress -o jsonpath="{.data.wordpress-password}" | base64 --decode)

==> v1beta1/Role
NAME           AGE
nginx-ingress  0s

==> v1beta1/RoleBinding
NAME           AGE
nginx-ingress  0s
```
