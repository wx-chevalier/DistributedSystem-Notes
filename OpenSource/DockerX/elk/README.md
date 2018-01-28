# Docker ELK Stack

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

# Acknowledge
