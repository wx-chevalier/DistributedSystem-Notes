# Chart

我们创建一个名为 mychart 的 Chart，看一看 Chart 的文件结构。

```sh
$ helm create mongodb
$ tree mongodb
mongodb
├── Chart.yaml #Chart本身的版本和配置信息
├── charts #依赖的chart
├── templates #配置模板目录
│   ├── NOTES.txt #helm提示信息
│   ├── _helpers.tpl #用于修改kubernetes objcet配置的模板
│   ├── deployment.yaml #kubernetes Deployment object
│   └── service.yaml #kubernetes Serivce
└── values.yaml #kubernetes object configuration

2 directories, 6 files
```

# 模板

Templates 目录下是 yaml 文件的模板，遵循 Go template 语法。使用过 Hugo 的静态网站生成工具的人应该对此很熟悉。我们查看下 deployment.yaml 文件的内容。

```yml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: {{ template "fullname" . }}
  labels:
    chart: "{{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}"
spec:
  replicas: {{ .Values.replicaCount }}
  template:
    metadata:
      labels:
        app: {{ template "fullname" . }}
    spec:
      containers:
      - name: {{ .Chart.Name }}
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        imagePullPolicy: {{ .Values.image.pullPolicy }}
        ports:
        - containerPort: {{ .Values.service.internalPort }}
        livenessProbe:
          httpGet:
            path: /
            port: {{ .Values.service.internalPort }}
        readinessProbe:
          httpGet:
            path: /
            port: {{ .Values.service.internalPort }}
        resources:
{{ toyaml .Values.resources | indent 12 }}
```

这是该应用的 Deployment 的 yaml 配置文件，其中的双大括号包扩起来的部分是 Go template，其中的 Values 是在 values.yaml 文件中定义的：

```yml
# Default values for mychart.
# This is a yaml-formatted file.
# Declare variables to be passed into your templates.
replicaCount: 1
image:
  repository: nginx
  tag: stable
  pullPolicy: IfNotPresent
service:
  name: nginx
  type: ClusterIP
  externalPort: 80
  internalPort: 80
resources:
  limits:
    cpu: 100m
    memory: 128Mi
  requests:
    cpu: 100m
    memory: 128Mi
```

比如在 Deployment.yaml 中定义的容器镜像 `image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"` 其中的：

- .Values.image.repository 就是 nginx
- .Values.image.tag 就是 stable

以上两个变量值是在 create chart 的时候自动生成的默认值。我们将默认的镜像地址和 tag 改成我们自己的镜像 harbor-001.jimmysong.io/library/nginx:1.9。

# 检查配置和模板是否有效

当使用 kubernetes 部署应用的时候实际上讲 templates 渲染成最终的 kubernetes 能够识别的 yaml 格式。使用 `helm install --dry-run --debug <chart_dir>` 命令来验证 chart 配置。该输出中包含了模板的变量配置与最终渲染的 yaml 文件。

```yml
$ helm install --dry-run --debug mychart
Created tunnel using local port: '58406'
SERVER: "localhost:58406"
CHART PATH: /Users/jimmy/Workspace/github/bitnami/charts/incubator/mean/charts/mychart
NAME:   filled-seahorse
REVISION: 1
RELEASED: Tue Oct 24 18:57:13 2017
CHART: mychart-0.1.0
USER-SUPPLIED VALUES:
{}

COMPUTED VALUES:
image:
  pullPolicy: IfNotPresent
  repository: harbor-001.jimmysong.io/library/nginx
  tag: 1.9
replicaCount: 1
resources:
  limits:
    cpu: 100m
    memory: 128Mi
  requests:
    cpu: 100m
    memory: 128Mi
service:
  externalPort: 80
  internalPort: 80
  name: nginx
  type: ClusterIP

HOOKS:
MANIFEST:

---
# Source: mychart/templates/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: filled-seahorse-mychart
  labels:
    chart: "mychart-0.1.0"
spec:
  type: ClusterIP
  ports:
  - port: 80
    targetPort: 80
    protocol: TCP
    name: nginx
  selector:
    app: filled-seahorse-mychart

---
# Source: mychart/templates/deployment.yaml
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
  name: filled-seahorse-mychart
  labels:
    chart: "mychart-0.1.0"
spec:
  replicas: 1
  template:
    metadata:
      labels:
        app: filled-seahorse-mychart
    spec:
      containers:
      - name: mychart
        image: "harbor-001.jimmysong.io/library/nginx:1.9"
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 80
        livenessProbe:
          httpGet:
            path: /
            port: 80
        readinessProbe:
          httpGet:
            path: /
            port: 80
        resources:
            limits:
              cpu: 100m
              memory: 128Mi
            requests:
              cpu: 100m
              memory: 128Mi
```

我们可以看到 Deployment 和 Service 的名字前半截由两个随机的单词组成，最后才是我们在 values.yaml 中配置的值。

# 部署到 kubernetes

在 mychart 目录下执行下面的命令将 nginx 部署到 kubernetes 集群上。

```yml
helm install .
NAME:   eating-hound
LAST DEPLOYED: Wed Oct 25 14:58:15 2017
NAMESPACE: default
STATUS: DEPLOYED

RESOURCES:
==> v1/Service
NAME                  CLUSTER-IP     EXTERNAL-IP  PORT(S)  AGE
eating-hound-mychart  10.254.135.68  <none>       80/TCP   0s

==> extensions/v1beta1/Deployment
NAME                  DESIRED  CURRENT  UP-TO-DATE  AVAILABLE  AGE
eating-hound-mychart  1        1        1           0          0s


NOTES:
1. Get the application URL by running these commands:
  export POD_NAME=$(kubectl get pods --namespace default -l "app=eating-hound-mychart" -o jsonpath="{.items[0].metadata.name}")
  echo "Visit http://127.0.0.1:8080 to use your application"
  kubectl port-forward $POD_NAME 8080:80
```

现在 nginx 已经部署到 kubernetes 集群上，本地执行提示中的命令在本地主机上访问到 nginx 实例。

```sh
$ export POD_NAME=$(kubectl get pods --namespace default -l "app=eating-hound-mychart" -o jsonpath="{.items[0].metadata.name}")

$ echo "Visit http://127.0.0.1:8080 to use your application"

$ kubectl port-forward $POD_NAME 8080:80
```

在本地访问 `http://127.0.0.1:8080` 即可访问到 nginx。

# 依赖管理

所有使用 helm 部署的应用中如果没有特别指定 chart 的名字都会生成一个随机的 Release name，例如 romping-frog、sexy-newton 等，跟启动 docker 容器时候容器名字的命名规则相同，而真正的资源对象的名字是在 YAML 文件中定义的名字，我们成为 App name，两者连接起来才是资源对象的实际名字：Release name-App name。

而使用 helm chart 部署的包含依赖关系的应用，都会使用同一套 Release name，在配置 YAML 文件的时候一定要注意在做服务发现时需要配置的服务地址，如果使用环境变量的话，需要像下面这样配置。

```yml
env:
  - name: SERVICE_NAME
    value: '{{ .Release.Name }}-{{ .Values.image.env.SERVICE_NAME }}'
```

这是使用了 Go template 的语法。至于 `{{ .Values.image.env.SERVICE_NAME }}` 的值是从 values.yaml 文件中获取的，所以需要在 values.yaml 中增加如下配置：

```yml
image:
  env:
    SERVICE_NAME: k8s-app-monitor-test
```

## 本地依赖

在本地当前 chart 配置的目录下启动 helm server，我们不指定任何参数，直接使用默认端口启动。

```bash
helm serve
```

将该 repo 加入到 repo list 中。

```bash
helm repo add local http://localhost:8879
```

在浏览器中访问 [http://localhost:8879](http://localhost:8879/) 可以看到所有本地的 chart。

然后下载依赖到本地。

```bash
helm dependency update
```

这样所有的 chart 都会下载到本地的 `charts` 目录下。

## 共享

我们可以修改 Chart.yaml 中的 helm chart 配置信息，然后使用下列命令将 chart 打包成一个压缩文件。

```sh
$ helm package .
```

打包出 mychart-0.1.0.tgz 文件，我们可以在 requirements.yaml 中定义应用所依赖的 chart，例如定义对 mariadb 的依赖：

```yml
dependencies:
  - name: mariadb
    version: 0.6.0
    repository: https://kubernetes-charts.storage.googleapis.com
```

使用 helm lint .命令可以检查依赖和模板配置是否正确。
