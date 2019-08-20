# 应用部署

# Deployment

# Service

# Ingress

```sh
$ helm install --name nginx-ingress --set "rbac.create=true,controller.service.externalIPs[0]=172.19.157.1,controller.service.externalIPs[1]=172.19.157.2,controller.service.$
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

An example Ingress that makes use of the controller:

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

If TLS is enabled for the Ingress, a Secret containing the certificate and key must also be provided:

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
$ helm install --name wordpress-test --set "persistence.enabled=false,mariadb.persistence.enabled=false" stable/wordpress

NAME:   wordpress-test
...
```

利用如下命令可以获得 WordPress 的访问地址：

```sh
$ echo http://$(kubectl get svc wordpress-test-wordpress -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
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

# 链接

- https://auth0.com/blog/kubernetes-tutorial-step-by-step-introduction-to-basic-concepts/#How-to-Deploy-Your-First-Kubernetes-Application
