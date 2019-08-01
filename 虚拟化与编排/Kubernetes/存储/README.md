# K8s 存储

Docker Volume 是针对容器层面的存储抽象，其volume的生命周期是通过docker engine来维护的。K8s Volume 则是应用层面的存储抽象，在K8s中通过Pod的概念将一组具有超亲密关系的容器组合到了一起形成了一个服务实例，为了保证一个Pod中某一个容器异常退出，被kubelet重建拉起旧容器产生的重要数据不丢，以及同一个Pod的多个容器可以共享数据，K8s在Pod层面定义了存储卷，也即通过`.spec.volumes`来声明Pod所要使用的volume类型。

而且K8s通过CRI接口解耦和docker engine的耦合关系，所以K8s的volume的生命周期理所当然由K8s来管理，因此K8s本身也有自己的volume plugin扩展机制。Pod `.spec.volumes`中的Volume类型做一个分类：

- 本地存储：emptydir/hostpath等，主要使用Pod运行的node上的本地存储

- 网络存储：in-tree(内置): awsElasticBlockStore/gcePersistentDisk/nfs等，存储插件的实现代码是放在k8s代码仓库中的；out-of-tree(外置): flexvolume/csi等网络存储inline volume plugins，存储插件单独实现，特别是CSI是volume扩展机制的核心发展方向。

- Projected Volume: secret/configmap/downwardAPI/serviceAccountToken，将K8s集群中的一些配置信息以volume的方式挂载到Pod的容器中，也即应用可以通过POSIX接口来访问这些对象中的数据。

- PersistentVolumeClaim与PersistentVolume体系，K8s中将存储资源与计算资源分开管理的设计体系
