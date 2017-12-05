# Java application

## 编译镜像

```
docker build -t username/java8 .
```


## 发布镜像
docker tag username/java8 localhost:5000/username/java8
docker push localhost:5000/username/java8

## 运行镜像

需要设定的环境变量如下：

```
APP     必填   应用打包地址
ENTRY   可选   未指定时，需要打包的应用中包含启动脚本，本文档后面有解释
```

以 Spring 项目为例，设打包生成的文件为 `/some/path/dc.jar`，打包过程如下：

```
$ mkdir app
$ cp /some/path/dc.jar app/
$ tree app
app
|-- dc.jar
$ tar czf app.tar.gz app
```

上传部署运行：

```
# 然后上传
curl --user UserName:CustomPassword \
  -T ./app.tar.gz \
  http://deploy.hostname.cn/upload/ggzy/server/

# 远端运行，该步骤可以在 UI 界面控制
docker run -d \
-e "APP=http://deploy.hostname.cn/ggzy/server/app.tar.gz" \
-e "ENTRY=app/dc.jar" \
-p 8020:8020 \
username/node
```

**注意，如果打包时，app 目录下打入了 launcher.sh，将运行该脚本启动程序**

```
$ tree app
|-- launcher.sh
|-- dc.jar
|-- ...
```

每次有新的版本发布，只需要重新上传文件，然后在管理界面中重启镜像即可。