# DokuWiki

DokuWiki 是一个针对小公司文件需求而开发的 Wiki 引擎，用 PHP 语言开发。DokuWiki 基于文本存储，不需要数据库。使用 helm install 进行一键部署，并通过 ingress.enabled=true 和 ingress.hosts[0].name=wiki.hi-linux.com 参数启用 Ingress 特性和设置对应的主机名。

```sh

$ cd /home/k8s/charts/stable
$ helm install --name dokuwiki --set "ingress.enabled=true,ingress.hosts[0].name=wiki.hi-linux.com,persistence.enabled=false" dokuwiki

NAMESPACE: default
STATUS: DEPLOYED

RESOURCES:
==> v1beta1/Ingress
NAME               HOSTS              ADDRESS  PORTS  AGE
dokuwiki-dokuwiki  wiki.hi-linux.com  80       53s

==> v1/Pod(related)
NAME                                READY  STATUS   RESTARTS  AGE
dokuwiki-dokuwiki-747b45cddb-qt8l2  1/1    Running  0         53s

==> v1/Secret
NAME               TYPE    DATA  AGE
dokuwiki-dokuwiki  Opaque  1     54s

==> v1/Service
NAME               TYPE          CLUSTER-IP      EXTERNAL-IP  PORT(S)                   AGE
dokuwiki-dokuwiki  LoadBalancer  10.254.235.137  <pending>    80:8430/TCP,443:8848/TCP  54s

==> v1beta1/Deployment
NAME               DESIRED  CURRENT  UP-TO-DATE  AVAILABLE  AGE
dokuwiki-dokuwiki  1        1        1           1          54s

NOTES:

** Please be patient while the chart is being deployed **

1. Get the DokuWiki URL indicated on the Ingress Rule and associate it to your cluster external IP:

export CLUSTER_IP=$(minikube ip) # On Minikube. Use: `kubectl cluster-info` on others K8s clusters
export HOSTNAME=$(kubectl get ingress --namespace default dokuwiki-dokuwiki -o jsonpath='{.spec.rules[0].host}')
echo "Dokuwiki URL: http://$HOSTNAME/"
echo "$CLUSTER_IP  $HOSTNAME" | sudo tee -a /etc/hosts

2. Login with the following credentials

echo Username: user
echo Password: $(kubectl get secret --namespace default dokuwiki-dokuwiki -o jsonpath="{.data.dokuwiki-password}" | base64 --decode)
```

部署完成后，根据提示生成相应的登陆用户名和密码。

```sh
$ echo Username: user
Username: user

$ echo Password: $(kubectl get secret --namespace default dokuwiki-dokuwiki -o jsonpath="{.data.dokuwiki-password}" | base64 --decode)
Password: e2GrABBkwF
```

测试从各节点的宿主机 IP 访问应用，这里我们直接使用 Curl 命令进行访问。

```sh
$ curl -I  http://wiki.hi-linux.com/doku.php -x 192.168.100.211:80
HTTP/1.1 200 OK
Server: nginx/1.13.8
Date: Wed, 25 Jul 2018 05:16:02 GMT
Content-Type: text/html; charset=utf-8
Connection: keep-alive
Vary: Accept-Encoding
X-Powered-By: PHP/7.0.31
Vary: Cookie,Accept-Encoding
Set-Cookie: DokuWiki=k2clt6f2qe472ehsq6tcmh6v20; path=/; HttpOnly
Expires: Thu, 19 Nov 1981 08:52:00 GMT
Cache-Control: no-store, no-cache, must-revalidate
Pragma: no-cache
Set-Cookie: DW68700bfd16c2027de7de74a5a8202a6f=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; Max-Age=0; path=/; HttpOnly
X-UA-Compatible: IE=edge,chrome=1

$ curl -I  http://wiki.hi-linux.com/doku.php -x 192.168.100.212:80
HTTP/1.1 200 OK
Server: nginx/1.13.8
Date: Wed, 25 Jul 2018 05:18:13 GMT
Content-Type: text/html; charset=utf-8
Connection: keep-alive
Vary: Accept-Encoding
X-Powered-By: PHP/7.0.31
Vary: Cookie,Accept-Encoding
Set-Cookie: DokuWiki=ork8sv8qpurteblasuq3eb3nt2; path=/; HttpOnly
Expires: Thu, 19 Nov 1981 08:52:00 GMT
Cache-Control: no-store, no-cache, must-revalidate
Pragma: no-cache
Set-Cookie: DW68700bfd16c2027de7de74a5a8202a6f=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; Max-Age=0; path=/; HttpOnly

$ curl -I  http://wiki.hi-linux.com/doku.php -x 192.168.100.213:80
HTTP/1.1 200 OK
Server: nginx/1.13.8
Date: Wed, 25 Jul 2018 05:18:30 GMT
Content-Type: text/html; charset=utf-8
Connection: keep-alive
Vary: Accept-Encoding
X-Powered-By: PHP/7.0.31
Vary: Cookie,Accept-Encoding
Set-Cookie: DokuWiki=6ulgtsddqq3rlo0mriavj64jc4; path=/; HttpOnly
Expires: Thu, 19 Nov 1981 08:52:00 GMT
Cache-Control: no-store, no-cache, must-revalidate
Pragma: no-cache
Set-Cookie: DW68700bfd16c2027de7de74a5a8202a6f=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; Max-Age=0; path=/; HttpOnly
```

Curl 用法很多，你也可以使用下面方式来达到相同的效果。

```sh
$ curl -H "Host:wiki.hi-linux.com"  "http://192.168.100.211/doku.php"
```

当然你也可以在本地 hosts 文件中对 IP 和域名进行绑定后，通过浏览器访问该应用。
