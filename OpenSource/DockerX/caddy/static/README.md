## 编译镜像
docker build -t username/static .

## 发布镜像
docker tag username/static localhost:5000/username/static
docker push localhost:5000/username/static

## 运行镜像
```
# 这里的 Caddy 证书存放在了 $HOME/.caddy 目录下
docker run -d \
-e "APP=http://deploy.hostname.cn/static/app.tar.gz"
-p 1080:80 \
username/static
```