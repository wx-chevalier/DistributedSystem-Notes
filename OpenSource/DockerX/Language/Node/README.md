# 编译镜像

```
docker build -t username/node .
```


# 发布镜像
docker tag username/node localhost:5000/username/node
docker push localhost:5000/username/node

# 运行镜像

## 公共资源 APPServer

应用打包为 tar.gz 压缩包格式，必须附带 package.json 以进行依赖安装。

```
# 本地执行打包，注意，在应用目录下打包，生成的包体会被压缩到上一层中
tar -czf ../app.tar.gz ./

# 然后上传
curl --user username:xxxxxxxxConfig1314 \
  -T ./app.tar.gz \
  http://deploy.xxxxxxxx.cn/upload/ggzy-appserver/

# 远端运行，该步骤可以在 UI 界面控制
docker run -d \
-e "APP=http://deploy.xxxxxxxx.cn/ggzy-appserver/app.tar.gz" \
-e "ENTRY=app.bundle.js" \
-p 1399:1399 \
username/node
```

每次有新的版本发布，只需要重新上传文件，然后在管理界面中重启镜像即可。