# Docker ELK Stack

```
sysctl -w vm.max_map_count=262144

echo 'vm.max_map_count=262144' >> /etc/sysctl.conf
```

# Local Mode

本地模式，仅开启单节点，并且将数据映射到本地文件系统中。

```sh
# 编译
docker build -t username/elasticsearch-kibana .

# 运行，并且映射数据
chmod 777 /mnt/esdata

docker run -d -p 9200:9200 -p 5601:5601 -v /mnt/esdata:/home/elasticsearch/elasticsearch/data username/elasticsearch-kibana

# 发布
docker tag username/elasticsearch-kibana localhost:5000/username/elasticsearch-kibana
docker push localhost:5000/username/elasticsearch-kibana
```

# HA Mode

本地双节点高可用模式。

```sh
# 启动集群
$ docker-compose up

# 关闭集群，并且移除数据卷
docker-compose down -v
```

# Swarm HA Mode

```sh
# 部署节点
$ docker stack deploy -c $(pwd)/docker-compose.yml elk

# 访问 Kibana http://<worker-node-ip>:5601

# 列举服务
$ docker service ls

# 水平扩容
docker service update --replicas=3 <service_id>
```

# Acknowledge

- [swarm-elk](https://github.com/ahromis/swarm-elk): This repository uses the new Docker Compose v3 format to create an ELK stack using swarm mode.