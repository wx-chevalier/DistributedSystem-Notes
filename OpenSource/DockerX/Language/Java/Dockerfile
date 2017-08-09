FROM openjdk:8-jre-alpine
LABEL maintainer "Dragon <384924552@qq.com>"

# 暴露必须的接口
EXPOSE 80 1399 8000-12000

RUN apk add --update curl tar && \
    rm -rf /var/cache/apk/*

# 创建并且设置运行目录
RUN mkdir /opt/
RUN mkdir /opt/workspace
WORKDIR /opt/workspace

# 添加运行脚本
ADD run.sh /opt/workspace
RUN chmod +x /opt/workspace/run.sh

ENTRYPOINT ["/opt/workspace/run.sh"]