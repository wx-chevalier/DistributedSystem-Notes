# presto server

## 构建

```
docker build -t presto:0.179 ./
```

## 创建数据卷

```
docker create -v /presto/server --name presto-server-0.179 presto:0.179 /bin/true
```

## 使用该数据卷中的 presto 启动

```
docker run \
  --rm -it \
  -e "CONFIG=debug.config" \
  --volumes-from presto-server-0.179 \
  -v /path/to/loca/data:/presto/data \
  -p 127.0.0.1:7090:7090 \
  presto-launcher:0.1
```