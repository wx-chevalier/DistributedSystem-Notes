FROM node:alpine
MAINTAINER Dragon <384924552@qq.com>

# 安装必备的 curl
RUN apk add --update curl tini && \
    rm -rf /var/cache/apk/*

# 安装全局依赖
RUN npm install pm2 -g

# 暴露常用端口
EXPOSE 80 1399 8000-12000 43554

# 指定目录
RUN mkdir /opt/workspace
WORKDIR /opt/workspace

# 添加 run.sh 并修改权限
ADD run.sh /etc
RUN chmod +x /etc/run.sh

# 设置环境变量 NODE_ENV (默认为 ‘production’) 
ARG NODE_ENV 
ENV NODE_ENV ${NODE_ENV:-production} 
ENV ENTRY="app.js"

# Set tini as entrypoint 
ENTRYPOINT ["/sbin/tini", "--"] 
CMD ["/bin/sh","/etc/run.sh"]
