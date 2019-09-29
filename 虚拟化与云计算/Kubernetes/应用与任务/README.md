# K8s 中的应用与任务部署

参考[云原生](https://ngte-be.gitbook.io/i/?q=云原生)一篇中的定义，将云原生的组件映射为 Kubernetes 的原语（即 Kubernetes 里的各种资源对象和概念组合），可以得到如下图：

![云原生与 K8s 概念映射](https://i.postimg.cc/0NxdMnYn/image.png)

在 K8s 中的应用部署我们往往应该遵循如下的规则：

- 不要直接部署裸的 Pod。在 Pod 里增加 Readiness 和 Liveness 探针。给 Pod 这只 CPU 和内存资源限额。

- 为工作负载选择合适的 Controller。

- 定义多个 Namespace 来限制默认 Service 范围的可视性。在应用程序工作负载启动之前先启动 Service

- 使用 Deployment history 来回滚到历史版本。使用 ConfigMap 和 Secret 来存储配置。

- 使用 Init 容器确保应用程序被正确的初始化。配置 HPA 来动态扩展无状态工作负载。
