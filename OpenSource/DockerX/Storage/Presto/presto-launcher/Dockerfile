FROM openjdk:8-jre-alpine
MAINTAINER lotuc

ENV PRESTO_HOME=/presto/server

RUN apk add --update curl tar sudo rsync python wget && \
    rm -rf /var/cache/apk/*

RUN mkdir /presto
COPY presto-launcher.sh /presto/presto-launcher.sh
VOLUME ["/presto/data", "/presto/server"]

EXPOSE 7070

WORKDIR $PRESTO_HOME

ENTRYPOINT ["/bin/sh"]
CMD ["/presto/presto-launcher.sh", "start"]
