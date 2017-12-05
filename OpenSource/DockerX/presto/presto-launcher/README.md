# presto-launcher

## 构建

```sh
./scripts/build.sh
```

## 启动

```
./scripts/start.sh -e CONFIG=... -e PLUGIN=... -e NODE_ENVIRONMENT=...
```

## 环境变量：

```
CONFIG    必选    对应配置文件 http://deploy.hostname.cn/dragondc/presto/config/$CONFIG.tar.gz
                  解压后为目录 config
PLUGIN    可选    对应插件包 http://deploy.hostname.cn/dragondc/presto/plugin/$PLUGIN.tar.gz
                  解压后为目录 plugin

NODE_ENVIRONMENT  可选  默认为 production
```

presto 的配置 `node.properties` 会自动生成，另外，其中的 `node.id` 值在容器第一次启动时生成，以后重启容器该值将保持不变。
