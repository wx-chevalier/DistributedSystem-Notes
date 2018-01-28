FROM openjdk:jre-alpine

LABEL maintainer "wxyyxc1992 <384924552@qq.com>"

ENV ES_VERSION=5.4.2 \
    KIBANA_VERSION=5.4.2

RUN apk add --quiet --no-progress --no-cache bash nodejs wget \
 && adduser -D elasticsearch

# 指定目录
USER elasticsearch

WORKDIR /home/elasticsearch

# 下载并且安装 ElasticSearch
RUN wget -q -O - https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-${ES_VERSION}.tar.gz \
 |  tar -zx \
 && mv elasticsearch-${ES_VERSION} elasticsearch \
 && wget -q -O - https://artifacts.elastic.co/downloads/kibana/kibana-${KIBANA_VERSION}-linux-x86_64.tar.gz \
 |  tar -zx \
 && mv kibana-${KIBANA_VERSION}-linux-x86_64 kibana \
 && rm -f kibana/node/bin/node kibana/node/bin/npm \
 && ln -s $(which node) kibana/node/bin/node \
 && ln -s $(which npm) kibana/node/bin/npm

# 下载插件并且解压缩
RUN wget https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v5.4.2/elasticsearch-analysis-ik-5.4.2.zip \
    && mkdir -p /home/elasticsearch/elasticsearch/plugins/ik \
    && mkdir -p /home/elasticsearch/elasticsearch/data \
    && unzip elasticsearch-analysis-ik-${ES_VERSION}.zip -d /home/elasticsearch/elasticsearch/plugins/ik/ \
    && rm -rf elasticsearch-analysis-ik-${ES_VERSION}.zip

# 暴露接口
EXPOSE 9200 5601

# 修正映射 Volume 的权限
VOLUME /home/elasticsearch/elasticsearch/data

RUN chown -R elasticsearch:elasticsearch /home/elasticsearch/elasticsearch/data

# 执行启动命令
CMD sh elasticsearch/bin/elasticsearch -E http.host=0.0.0.0 --quiet & kibana/bin/kibana --host 0.0.0.0 -Q

