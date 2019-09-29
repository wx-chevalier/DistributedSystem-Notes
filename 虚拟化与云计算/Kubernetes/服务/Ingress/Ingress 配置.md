# Ingress 配置

# 使用外部服务

## 外部 IP

在部分场景下我们需要使用外部的 IP 作为 Ingress 的后端服务，此时我们仍需要创建一个指向外部服务的 Service：

```yml
apiVersion: v1
kind: Service
metadata:
  name: release-name-ufc-rancher
  labels:
    app.kubernetes.io/name: ufc-rancher
    helm.sh/chart: ufc-rancher-0.1.0
    app.kubernetes.io/instance: release-name
    app.kubernetes.io/version: '1.0'
    app.kubernetes.io/managed-by: Tiller
spec:
  type: ClusterIP
  ports:
    - port: 58080
      targetPort: 58080
      protocol: TCP
---
apiVersion: v1
kind: Endpoints
metadata:
  name: release-name-ufc-rancher
subsets:
  - addresses:
      # list all external ips for this service
      - ip: 172.19.157.3
    ports:
      - port: 58080
        protocol: TCP
```

值得注意的是，这里我们在声明服务的时候并未指明 Pod Selector，这也就创建了一个没有后端的 Service，我们需要手动地去创建某个 Endpoints 然后将流量导入到该 Endpoints。外部对服务的访问则是同样创建 Ingress 资源即可：

```yml
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: release-name-ufc-rancher
  labels:
    app.kubernetes.io/name: ufc-rancher
    helm.sh/chart: ufc-rancher-0.1.0
    app.kubernetes.io/instance: release-name
    app.kubernetes.io/version: '1.0'
    app.kubernetes.io/managed-by: Tiller
  annotations:
    certmanager.k8s.io/issuer: letsencrypt-prod
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/proxy-body-size: '0'

spec:
  tls:
    - hosts:
        - 'k8s.unionfab.com'
      secretName: ufc-rancher-tls
  rules:
    # ufc rancher ingress rules
    - host: 'k8s.unionfab.com'
      http:
        paths:
          - path: /
            backend:
              serviceName: release-name-ufc-rancher
              servicePort: 58080
```

这里我们的实例可以参考使用 Ingress 以允许用域名方式访问 Rancher，其 Helm 配置参考 [K8s/Helm](https://github.com/wx-chevalier/Backend-Boilerplates)。

## 外部域名

# 链接

- https://appscode.com/products/voyager/10.0.0/guides/ingress/http/external-svc/
