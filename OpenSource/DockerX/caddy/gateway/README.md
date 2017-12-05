# Caddy/Gateway

## 更新 Gateway 配置

简单的透明代理请直接编辑 `transparentProxy.json` 后在仓库根目录运行（该脚本详细使用方法请阅读仓库根目录下 scripts/README.md）

```sh
node scripts/gatewayUtils.js
```

## 编译镜像

```
sh scripts/build.sh
```

## 发布镜像

```
sh scripts/push.sh
```

## 运行镜像

Caddy 证书存放在 $HOME/.caddy 目录下。

注意镜像将以 CaddyGateway 命名，如果已经存在，该脚本仅重启该容器，如果不存在则创建并启动。

```
sh scripts/start.sh
```

## 上传文件
curl --user UserName:CustomPassword \
  -T ./NetpasHelper.log \
  http://deploy.hostname.cn/upload/test/test
```
