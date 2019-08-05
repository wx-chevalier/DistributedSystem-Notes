# K8s 存储

Docker Volume 是针对容器层面的存储抽象，其 volume 的生命周期是通过 docker engine 来维护的。K8s Volume 则是应用层面的存储抽象，在 K8s 中通过 Pod 的概念将一组具有超亲密关系的容器组合到了一起形成了一个服务实例，为了保证一个 Pod 中某一个容器异常退出，被 kubelet 重建拉起旧容器产生的重要数据不丢，以及同一个 Pod 的多个容器可以共享数据，K8s 在 Pod 层面定义了存储卷，也即通过`.spec.volumes`来声明 Pod 所要使用的 volume 类型。

而且 K8s 通过 CRI 接口解耦和 docker engine 的耦合关系，所以 K8s 的 volume 的生命周期理所当然由 K8s 来管理，因此 K8s 本身也有自己的 volume plugin 扩展机制。Pod `.spec.volumes`中的 Volume 类型做一个分类：

- 本地存储：emptydir/hostpath 等，主要使用 Pod 运行的 node 上的本地存储

- 网络存储：in-tree(内置): awsElasticBlockStore/gcePersistentDisk/nfs 等，存储插件的实现代码是放在 k8s 代码仓库中的；out-of-tree(外置): flexvolume/csi 等网络存储 inline volume plugins，存储插件单独实现，特别是 CSI 是 volume 扩展机制的核心发展方向。

- Projected Volume: secret/configmap/downwardAPI/serviceAccountToken，将 K8s 集群中的一些配置信息以 volume 的方式挂载到 Pod 的容器中，也即应用可以通过 POSIX 接口来访问这些对象中的数据。

- PersistentVolumeClaim 与 PersistentVolume 体系，K8s 中将存储资源与计算资源分开管理的设计体系
