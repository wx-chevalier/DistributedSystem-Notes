# Deployment

Deployment 为 Pod 和 ReplicaSet 提供了一个声明式定义方法，用来替代以前的 Replication Controller 来方便的管理应用。只需要在 Deployment 中描述您想要的目标状态是什么，Deployment Controller 就会将 Pod 和 ReplicaSet 的实际状态改变到目标状态。您可以定义一个全新的 Deployment 来创建 ReplicaSet 或者删除已有的 Deployment 并创建一个新的来替换。

Deployment 典型的应用场景是，使用 Deployment 来创建 ReplicaSet。ReplicaSet 在后台创建 Pod 并检查启动状态，看它是成功还是失败。然后，通过更新 Deployment 的 PodTemplateSpec 字段来声明 Pod 的新状态。这会创建一个新的 ReplicaSet。Deployment 会按照控制的速率将 pod 从旧的 ReplicaSet 移动到新的 ReplicaSet 中；如果当前状态不稳定，回滚到之前的 Deployment revision。每次回滚都会更新 Deployment 的 revision。Deployment 还能够完成扩容 Deployment 以满足更高的负载；暂停 Deployment 来应用 PodTemplateSpec 的多个修复，然后恢复上线；根据 Deployment 的状态判断上线是否 hang 住了；清除旧的不必要的 ReplicaSet 等功能。

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
$ kubectl create -f https://raw.githubusercontent.com/kubernetes-client/python/master/examples/nginx-deployment.yaml --record
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

## 扩容

您可以使用以下命令扩容 Deployment：

```sh
$ kubectl scale deployment nginx-deployment --replicas 10
deployment "nginx-deployment" scaled
```

假设您的集群中启用了 horizontal pod autoscaling，您可以给 Deployment 设置一个 autoscaler，基于当前 Pod 的 CPU 利用率选择最少和最多的 Pod 数。

```sh
$ kubectl autoscale deployment nginx-deployment --min=10 --max=15 --cpu-percent=80
deployment "nginx-deployment" autoscaled
```

RollingUpdate Deployment 支持同时运行一个应用的多个版本。或者 autoscaler 扩 容 RollingUpdate Deployment 的时候，正在中途的 rollout（进行中或者已经暂停的），为了降低风险，Deployment controller 将会平衡已存在的活动中的 ReplicaSet（有 Pod 的 ReplicaSet）和新加入的 replica。这被称为比例扩容。

譬如正在运行中含有 10 个 replica 的 Deployment。maxSurge=3，maxUnavailable=2：

```sh
$ kubectl get deploy
NAME                 DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
nginx-deployment     10        10        10           10          50s

# 更新了错误的镜像
$ kubectl set image deploy/nginx-deployment nginx=nginx:sometag
deployment "nginx-deployment" image updated

# 启动了一个包含 ReplicaSet nginx-deployment-1989198191 的新的 rollout，但是它被阻塞
$ kubectl get rs
NAME                          DESIRED   CURRENT   READY     AGE
nginx-deployment-1989198191   5         5         0         9s
nginx-deployment-618515232    8         8         8         1m
```

autoscaler 将 Deployment 的 repllica 数目增加到了 15 个。Deployment controller 需要判断在哪里增加这 5 个新的 replica。如果我们没有谁用比例扩容，所有的 5 个 replica 都会加到一个新的 ReplicaSet 中。如果使用比例扩容，新添加的 replica 将传播到所有的 ReplicaSet 中。大的部分加入 replica 数最多的 ReplicaSet 中，小的部分加入到 replica 数少的 ReplciaSet 中。0 个 replica 的 ReplicaSet 不会被扩容。在我们上面的例子中，3 个 replica 将添加到旧的 ReplicaSet 中，2 个 replica 将添加到新的 ReplicaSet 中。rollout 进程最终会将所有的 replica 移动到新的 ReplicaSet 中，假设新的 replica 成为健康状态。

```sh
$ kubectl get deploy
NAME                 DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
nginx-deployment     15        18        7            8           7m
$ kubectl get rs
NAME                          DESIRED   CURRENT   READY     AGE
nginx-deployment-1989198191   7         7         0         7m
nginx-deployment-618515232    11        11        11        7m
```

## 更新

Kubernetes 提供了多种升级方案，Recreate 即删除所有已存在的 pod,重新创建新的; RollingUpdate 即滚动升级，逐步替换的策略，同时滚动升级时，支持更多的附加参数，例如设置最大不可用 Pod 数量，最小升级间隔时间等等。Deployment 的 rollout 当且仅当 Deployment 的 pod template（例如.spec.template）中的 label 更新或者镜像更改时被触发。其他更新，例如扩容 Deployment 不会触发 rollout。

```sh
# 让 nginx pod 使用nginx:1.9.1的镜像来代替原来的nginx:1.7.9的镜像
$ kubectl set image deployment/nginx-deployment nginx=nginx:1.9.1
deployment "nginx-deployment" image updated
```

我们可以使用 edit 命令来编辑 Deployment，修改 .spec.template.spec.containers[0].image ，将 nginx:1.7.9 改写成 nginx:1.9.1。

```sh
$ kubectl edit deployment/nginx-deployment
deployment "nginx-deployment" edited

# 查看 rollout 的状态
$ kubectl rollout status deployment/nginx-deployment
Waiting for rollout to finish: 2 out of 3 new replicas have been updated...
deployment "nginx-deployment" successfully rolled out

# 查看关联的 ReplicaSet 信息
$ kubectl get rs
NAME                          DESIRED   CURRENT   READY   AGE
nginx-deployment-1564180365   3         3         0       6s
nginx-deployment-2035384211   0         0         0       36s
```

我们通过执行 kubectl get rs 可以看到 Deployment 更新了 Pod，通过创建一个新的 ReplicaSet 并扩容了 3 个 replica，同时将原来的 ReplicaSet 缩容到了 0 个 replica。执行 get pods 只会看到当前的新的 pod。

Deployment 可以保证在升级时只有一定数量的 Pod 是 down 的。默认的，它会确保至少有比期望的 Pod 数量少一个是 up 状态（最多一个不可用）。Deployment 同时也可以确保只创建出超过期望数量的一定数量的 Pod。默认的，它会确保最多比期望的 Pod 数量多一个的 Pod 是 up 的（最多 1 个 surge ）。

## 回滚

有时候您可能想回退一个 Deployment，例如，当 Deployment 不稳定时，比如一直 crash looping。默认情况下，kubernetes 会在系统中保存前两次的 Deployment 的 rollout 历史记录，以便您可以随时回退（您可以修改 revision history limit 来更改保存的 revision 数）。

只要 Deployment 的 rollout 被触发就会创建一个 revision。也就是说当且仅当 Deployment 的 Pod template（如.spec.template）被更改，例如更新 template 中的 label 和容器镜像时，就会创建出一个新的 revision。其他的更新，比如扩容 Deployment 不会创建 revision，因此我们可以很方便的手动或者自动扩容。这意味着当您回退到历史 revision 时，只有 Deployment 中的 Pod template 部分才会回退。

```sh
# 检查下 Deployment 的 revision
$ kubectl rollout history deployment/nginx-deployment
deployments "nginx-deployment":
REVISION    CHANGE-CAUSE
1           kubectl create -f https://kubernetes.io/docs/user-guide/nginx-deployment.yaml--record
2           kubectl set image deployment/nginx-deployment nginx=nginx:1.9.1
3           kubectl set image deployment/nginx-deployment nginx=nginx:1.91
```

因为我们创建 Deployment 的时候使用了--record 参数可以记录命令，我们可以很方便的查看每次 revision 的变化。查看单个 revision 的详细信息：

```sh
$ kubectl rollout history deployment/nginx-deployment --revision=2
deployments "nginx-deployment" revision 2
  Labels:       app=nginx
          pod-template-hash=1159050644
  Annotations:  kubernetes.io/change-cause=kubectl set image deployment/nginx-deployment nginx=nginx:1.9.1
  Containers:
   nginx:
    Image:      nginx:1.9.1
    Port:       80/TCP
     QoS Tier:
        cpu:      BestEffort
        memory:   BestEffort
    Environment Variables:      <none>
  No volumes.
```

同样可以通过 rollout 命令来回退版本：

```sh
# 回退当前的 rollout 到之前的版本
$ kubectl rollout undo deployment/nginx-deployment
deployment "nginx-deployment" rolled back

# 使用 --revision参数指定某个历史版本
$ kubectl rollout undo deployment/nginx-deployment --to-revision=2
deployment "nginx-deployment" rolled back
```

可以通过设置 `.spec.revisonHistoryLimit` 项来指定 deployment 最多保留多少 revision 历史记录。默认的会保留所有的 revision；如果将该项设置为 0，Deployment 就不允许回退了。

## 暂停和恢复

您可以在发出一次或多次更新前暂停一个 Deployment，然后再恢复它。这样您就能在 Deployment 暂停期间进行多次修复工作，而不会发出不必要的 rollout。例如使用刚刚创建 Deployment：

```sh
$ kubectl get deploy
NAME      DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
nginx     3         3         3            3           1m
[mkargaki@dhcp129-211 kubernetes]$ kubectl get rs
NAME               DESIRED   CURRENT   READY     AGE
nginx-2142116321   3         3         3         1m
```

使用以下命令暂停 Deployment：

```sh
$ kubectl rollout pause deployment/nginx-deployment
deployment "nginx-deployment" paused
```

然后更新 Deplyment 中的镜像：

```sh
$ kubectl set image deploy/nginx nginx=nginx:1.9.1
deployment "nginx-deployment" image updated
```

注意新的 rollout 启动了：

```sh
$ kubectl rollout history deploy/nginx
deployments "nginx"
REVISION  CHANGE-CAUSE
1   <none>

$ kubectl get rs
NAME               DESIRED   CURRENT   READY     AGE
nginx-2142116321   3         3         3         2m
```

您可以进行任意多次更新，例如更新使用的资源：

```sh
$ kubectl set resources deployment nginx -c=nginx --limits=cpu=200m,memory=512Mi
deployment "nginx" resource requirements updated
Deployment 暂停前的初始状态将继续它的功能，而不会对 Deployment 的更新产生任何影响，只要 Deployment是暂停的。
```

最后，恢复这个 Deployment，观察完成更新的 ReplicaSet 已经创建出来了：

```sh
$ kubectl rollout resume deploy nginx
deployment "nginx" resumed
```
