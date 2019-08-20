# 基于 Ubuntu 的 Kubernetes 集群搭建

推荐首先使用 [Minikube](https://kubernetes.io/docs/tasks/tools/install-minikube/) 搭建简单的本地化集群，其需要依次安装 [VirtualBox](https://www.virtualbox.org/wiki/Downloads), [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) 以及 [minikube](https://github.com/kubernetes/minikube/releases) 等工具；在生产环境下，我们常常需要离线安装，此时可以参考[离线安装 K8S](https://parg.co/AT5)。

# 镜像解析

gcr.io 的很多镜像国内不便于访问，有同学将 [gcr.io 相关镜像](https://github.com/anjia0532/gcr.io_mirror) pull 下来，然后 push 到 docker 官方仓库，相关转换语法如下：

```sh
gcr.io/namespace/image_name:image_tag
# 等价于
anjia0532/namespace.image_name:image_tag

# 特别的
k8s.gcr.io/{image}/{tag} <==> gcr.io/google-containers/{image}/{tag} <==> anjia0532/google-containers.{image}/{tag}
```

批量转换的脚本如下：

```sh
# replace gcr.io/google-containers/federation-controller-manager-arm64:v1.3.1-beta.1 to real image
# this will convert gcr.io/google-containers/federation-controller-manager-arm64:v1.3.1-beta.1
# to anjia0532/google-containers.federation-controller-manager-arm64:v1.3.1-beta.1 and pull it
# k8s.gcr.io/{image}/{tag} <==> gcr.io/google-containers/{image}/{tag} <==> anjia0532/google-containers.{image}/{tag}

images=$(cat img.txt)
#or
#images=$(cat <<EOF
# gcr.io/google-containers/federation-controller-manager-arm64:v1.3.1-beta.1
# gcr.io/google-containers/federation-controller-manager-arm64:v1.3.1-beta.1
# gcr.io/google-containers/federation-controller-manager-arm64:v1.3.1-beta.1
#EOF
#)

eval $(echo ${images}|
        sed 's/k8s\.gcr\.io/anjia0532\/google-containers/g;s/gcr\.io/anjia0532/g;s/\//\./g;s/ /\n/g;s/anjia0532\./anjia0532\//g' |
        uniq |
        awk '{print "docker pull "$1";"}'
       )

# this code will retag all of anjia0532's image from local  e.g. anjia0532/google-containers.federation-controller-manager-arm64:v1.3.1-beta.1
# to gcr.io/google-containers/federation-controller-manager-arm64:v1.3.1-beta.1
# k8s.gcr.io/{image}/{tag} <==> gcr.io/google-containers/{image}/{tag} <==> anjia0532/google-containers.{image}/{tag}

for img in $(docker images --format "{{.Repository}}:{{.Tag}}"| grep "anjia0532"); do
  n=$(echo ${img}| awk -F'[/.:]' '{printf "gcr.io/%s",$2}')
  image=$(echo ${img}| awk -F'[/.:]' '{printf "/%s",$3}')
  tag=$(echo ${img}| awk -F'[:]' '{printf ":%s",$2}')
  docker tag $img "${n}${image}${tag}"
  [[ ${n} == "gcr.io/google-containers" ]] && docker tag $img "k8s.gcr.io${image}${tag}"
done
```

# kubelet

kubeadm 用于搭建并启动一个集群，kubelet 用于集群中所有节点上都有的用于做诸如启动 pod 或容器这种事情，kubectl 则是与集群交互的命令行接口。kubelet 和 kubectl 并不会随 kubeadm 安装而自动安装，需要手工安装。

在安装 kubeadm 时候，如果碰到需要翻墙的情况，可以使用 USTC 的源：

```sh
# 添加源并且更新
$ vim /etc/apt/sources.list.d/kubernetes.list
# 添加如下行：
# deb http://mirrors.ustc.edu.cn/kubernetes/apt/ kubernetes-xenial main
$ apt-get update

$ apt-get install -y kubelet kubeadm kubectl --allow-unauthenticated
$ apt-mark hold kubelet kubeadm kubectl
```

配置 cgroup driver, 保证和 docker 的一样:

```sh
$ docker info | grep -i cgroup

# 编辑配置文件
$ vim /etc/default/kubelet

# 添加如下配置
KUBELET_KUBEADM_EXTRA_ARGS=--cgroup-driver=<value>

# 配置修改后重启
$ systemctl daemon-reload
$ systemctl restart kubelet
```

# kubeadm 集群初始化

kubeadm 安装完毕后，可以初始化 Master 节点：

```sh
$ kubeadm init

# 如果存在网络问题，则可以使用代理访问
$ HTTP_PROXY=127.0.0.1:8118 HTTPS_PROXY=127.0.0.1:8118 kubeadm init

# 接下来我们还需要设置配置文件以最终启动集群
$ mkdir -p $HOME/.kube
$ sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
$ sudo chown $(id -u):$(id -g) $HOME/.kube/config

# 或者 Root 用户还可以添加如下映射
$ export KUBECONFIG=/etc/kubernetes/admin.conf
```

值得一提的是，如果无法通过代理访问，还可以使用国内的镜像数据，可以使用[如下脚本](https://github.com/anjia0532/gcr.io_mirror)便捷录取墙外镜像:

```sh
# credits: https://github.com/anjia0532/gcr.io_mirror

images=$(cat img.txt)

eval $(echo ${images}|
        sed 's/k8s\.gcr\.io/anjia0532\/google-containers/g;s/gcr\.io/anjia0532/g;s/\//\./g;s/ /\n/g;s/anjia0532\./anjia0532\//g' |
        uniq |
        awk '{print "docker pull "$1";"}'
       )

for img in $(docker images --format "{{.Repository}}:{{.Tag}}"| grep "anjia0532"); do
  n=$(echo ${img}| awk -F'[/.:]' '{printf "gcr.io/%s",$2}')
  image=$(echo ${img}| awk -F'[/.:]' '{printf "/%s",$3}')
  tag=$(echo ${img}| awk -F'[:]' '{printf ":%s",$2}')
  docker tag $img "${n}${image}${tag}"
  [[ ${n} == "gcr.io/google-containers" ]] && docker tag $img "k8s.gcr.io${image}${tag}"
done
```

# kubectl

Master 节点初始化完毕后，我们需要加入工作节点，或者设置 Master 节点上可调度 Pods

```sh
# 如果是单机节点，要在 Master 机器上调度 Pods，还需解锁 Master 限制
$ kubectl taint nodes --all node-role.kubernetes.io/master-

# 创建并且打印出工作节点加入集群的命令
$ sudo kubeadm token create --print-join-command

# 查看 Token 列表
$ kubeadm token list

# 工作节点加入集群
$ kubeadm join --token <token> <master-ip>:<master-port> --discovery-token-ca-cert-hash sha256:<hash>
```

# 网络配置

我们还需要配置节点间通信的网络:

```sh
# 安装 Weave 网络
$ kubectl apply -f "https://cloud.weave.works/k8s/net?k8s-version=$(kubectl version | base64 | tr -d '\n')"

# 或者安装 Flannel 网络
$ kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
$ kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/k8s-manifests/kube-flannel-rbac.yml
```

我们也可以使用自定义的配置文件来配置 K8S 集群，譬如可以手工指定默认网关使用的网络接口，完整配置文件可以参考[这里](https://kubernetes.io/docs/reference/setup-tools/kubeadm/kubeadm-init/#config-file):

```yaml
apiVersion: kubeadm.k8s.io/v1alpha1
kind: MasterConfiguration
networking:
  podSubnet: 10.244.0.0/16 # 使用 flannel
```

可以配置 kubernetes-dashboard 作为首个服务:

```sh
# 安装 Dashboard
$ kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/master/src/deploy/recommended/kubernetes-dashboard.yaml

$ kubectl proxy --address 0.0.0.0 --accept-hosts '.*'

# http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/
```
