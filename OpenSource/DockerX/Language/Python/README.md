# 编译镜像

```
docker build -t username/python3 .
```


# 发布镜像
```
# 在编译机器上
docker tag username/python3 localhost:5000/username/python3
docker push localhost:5000/username/python3

# 在目标机器上
docker pull $HOST/username/python3
```

# 部署应用

应用打包为 tar.gz 压缩包格式，必须附带 requirements 以进行依赖安装。

```
# 本地执行打包，注意，在应用目录下打包，生成的包体会被压缩到上一层中
tar -czf ../app.tar.gz ./

# 然后上传
curl --user UserName:CustomPassword \
  -T ./app.tar.gz \
  http://deploy.hostname.cn/upload/12345/datamining/

# 远端运行，该步骤可以在 UI 界面控制
docker run -d \
-e "APP=http://deploy.hostname.cn/12345/datamining/app.tar.gz" \
-e "ENTRY=api.python" \
-p 8050:8050 \
username/python3
```

每次有新的版本发布，只需要重新上传文件，然后在管理界面中重启镜像即可。