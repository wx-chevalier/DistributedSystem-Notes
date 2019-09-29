# Minio

Minio 是一个基于 Apache License v2.0 开源协议的对象存储服务，Minio 使用 Go 语言开发，具有良好的跨平台性，同样是一个非常轻量的服务。它兼容亚马逊 S3 云存储服务接口，非常适合于存储大容量非结构化的数据，例如：图片、视频、日志文件、备份数据和容器/虚拟机镜像等。

使用 helm install 进行一键部署，并通过 ingress.enabled=true 参数启用 Ingress 特性。

```sh
$ helm install --name minio  --set "ingress.enabled=true,persistence.enabled=false" minio

NAMESPACE: default
STATUS: DEPLOYED

RESOURCES:
==> v1beta1/Ingress
NAME   HOSTS               ADDRESS  PORTS  AGE
minio  minio.hi-linux.com  80       1m

==> v1/Pod(related)
NAME                   READY  STATUS   RESTARTS  AGE
minio-7c7cf49d4-gqf8p  1/1    Running  0         1m

==> v1/Secret
NAME   TYPE    DATA  AGE
minio  Opaque  2     1m

==> v1/ConfigMap
NAME   DATA  AGE
minio  2     1m

==> v1/Service
NAME   TYPE       CLUSTER-IP  EXTERNAL-IP  PORT(S)   AGE
minio  ClusterIP  None        <none>       9000/TCP  1m

==> v1beta2/Deployment
NAME   DESIRED  CURRENT  UP-TO-DATE  AVAILABLE  AGE
minio  1        1        1           1          1m

NOTES:

Minio can be accessed via port 9000 on the following DNS name from within your cluster:
minio-svc.default.svc.cluster.local

To access Minio from localhost, run the below commands:

1. export POD_NAME=$(kubectl get pods --namespace default -l "release=minio" -o jsonpath="{.items[0].metadata.name}")

2. kubectl port-forward $POD_NAME 9000 --namespace default

Read more about port forwarding here: http://kubernetes.io/docs/user-guide/kubectl/kubectl_port-forward/

You can now access Minio server on http://localhost:9000. Follow the below steps to connect to Minio server with mc client:

1. Download the Minio mc client - https://docs.minio.io/docs/minio-client-quickstart-guide

2. mc config host add minio-local http://localhost:9000 AKIAIOSFODNN7EXAMPLE wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY S3v4

3. mc ls minio-local

Alternately, you can use your browser or the Minio SDK to access the server - https://docs.minio.io/categories/17
```

部署完成后，我们在本地 hosts 文件中对 IP 和域名进行绑定，并通过浏览器访问该应用。登陆用户名和密码在部署完成后的提示信息中。最后我们在 Kubernetes 上来查看下部署成功后的 Ingress 信息。

```sh
$ kubectl get ingress
NAME                HOSTS                ADDRESS   PORTS     AGE
dokuwiki-dokuwiki   wiki.hi-linux.com              80        44m
minio               minio.hi-linux.com             80        50s
```
