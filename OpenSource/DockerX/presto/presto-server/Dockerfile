FROM alpine:latest
MAINTAINER lotuc

ENV PRESTO_VERSION=0.179
ENV PRESTO_HOME=/presto/server

RUN apk add --update curl tar \
    && rm -rf /var/cache/apk/*

RUN mkdir -p ${PRESTO_HOME}
WORKDIR ${PRESTO_HOME}

RUN curl --user UserName:CustomPassword \
         http://deploy.hostname.cn/dragondc/presto/server/presto-server-${PRESTO_VERSION}.tar.gz \
         -o presto-server-${PRESTO_VERSION}.tar.gz \
    && tar xfz presto-server-${PRESTO_VERSION}.tar.gz \
    && mv presto-server-${PRESTO_VERSION}/* ./ \
    && rm -rf presto-server-${PRESTO_VERSION} presto-server-${PRESTO_VERSION}.tar.gz
