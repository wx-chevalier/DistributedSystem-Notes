# Deployment

Deployment 为 Pod 和 ReplicaSet 提供了一个声明式定义(declarative)方法，用来替代以前的 ReplicationController 来方便的管理应用。只需要在 Deployment 中描述您想要的目标状态是什么，Deployment controller 就会将 Pod 和 ReplicaSet 的实际状态改变到目标状态。您可以定义一个全新的 Deployment 来创建 ReplicaSet 或者删除已有的 Deployment 并创建一个新的来替换。典型的应用场景包括：

- 使用 Deployment 来创建 ReplicaSet。ReplicaSet 在后台创建 pod。检查启动状态，看它是成功还是失败。

- 然后，通过更新 Deployment 的 PodTemplateSpec 字段来声明 Pod 的新状态。这会创建一个新的 ReplicaSet，

- Deployment 会按照控制的速率将 pod 从旧的 ReplicaSet 移动到新的 ReplicaSet 中。

- 如果当前状态不稳定，回滚到之前的 Deployment revision。每次回滚都会更新 Deployment 的 revision。

- 扩容 Deployment 以满足更高的负载。

- 暂停 Deployment 来应用 PodTemplateSpec 的多个修复，然后恢复上线。

- 根据 Deployment 的状态判断上线是否 hang 住了。

- 清除旧的不必要的 ReplicaSet。

# Deployment 操作

## 创建

比如一个简单的 Nginx 应用可以定义为：

```yml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  replicas: 3
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
        - name: nginx
          image: nginx:1.7.9
          ports:
            - containerPort: 80
```

然后通过如下操作进行创建：

```sh
$ kubectl create -f https://kubernetes.io/docs/user-guide/nginx-deployment.yaml --record
deployment "nginx-deployment" created

$ kubectl get deployments
NAME               DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
nginx-deployment   3         0         0            0           1s

# 等待一段时间
$ kubectl get deployments
NAME               DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
nginx-deployment   3         3         3            3           18s
```

可以看到 Deployment 已经创建了 3 个 replica，所有的 replica 都已经是最新的了（包含最新的 pod template），可用的（根据 Deployment 中的.spec.minReadySeconds 声明，处于已就绪状态的 pod 的最少个数）。执行 kubectl get rs 和 kubectl get pods 会显示 Replica Set（RS）和 Pod 已创建。

```sh
# ReplicaSet 的名字总是<Deployment的名字>-<pod template的hash值>。
$ kubectl get rs
NAME                          DESIRED   CURRENT   READY   AGE
nginx-deployment-2035384211   3         3         0       18s
```

## 更新

Kubernetes 提供了多种升级方案，Recreate 即删除所有已存在的 pod,重新创建新的; RollingUpdate 即滚动升级，逐步替换的策略，同时滚动升级时，支持更多的附加参数，例如设置最大不可用 Pod 数量，最小升级间隔时间等等。

## 回滚

Deployment 的 rollout 当且仅当 Deployment 的 pod template（例如.spec.template）中的 label 更新或者镜像更改时被触发。其他更新，例如扩容 Deployment 不会触发 rollout。假如我们现在想要让 Nginx pod 使用 nginx:1.9.1 的镜像来代替原来的 nginx:1.7.9 的镜像。
