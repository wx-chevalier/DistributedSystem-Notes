# 使用该Hadoop镜像
FROM sequenceiq/hadoop-docker:2.7.1

MAINTAINER WxChevalier

#Based on Inmobi Hive
#Builds the InMobi Hive from trunk
#Configure Postgres DB
#Starts Hive metastore Server
#Starts Hive Server2

# explicitly set user/group IDs
RUN groupadd -r postgres --gid=999 && useradd -r -g postgres --uid=999 postgres

# 安装Postgres作为Hive元数据存储
RUN yum update -y
RUN yum -y install vim postgresql-9.3 libpostgresql-jdbc-java

# 创建元数据库 创建Hive用户并且分配权限privileges
USER postgres
RUN /etc/init.d/postgresql start &&\
     psql --command "CREATE DATABASE metastore;" &&\
     psql --command "CREATE USER hive WITH PASSWORD 'hive';" && \
     psql --command "ALTER USER hive WITH SUPERUSER;" && \
     psql --command "GRANT ALL PRIVILEGES ON DATABASE metastore TO hive;"
     
# 切回到默认的root用户
USER root

# 构建开发工具
RUN yum update -y
RUN yum install -y git libprotobuf-dev protobuf-compiler

# 安装Maven
RUN curl -s http://mirror.olnevhost.net/pub/apache/maven/binaries/apache-maven-3.2.1-bin.tar.gz | tar -xz -C /usr/local/
RUN cd /usr/local && ln -s apache-maven-3.2.1 maven
ENV MAVEN_HOME /usr/local/maven
ENV PATH $MAVEN_HOME/bin:$PATH

            
# 下载并且编译Hive
ENV HIVE_VERSION 0.13.4-inm-SNAPSHOT
RUN cd /usr/local && git clone https://github.com/InMobi/hive.git
RUN cd /usr/local/hive && /usr/local/maven/bin/mvn clean install -DskipTests -Phadoop-2,dist
RUN mkdir /usr/local/hive-dist && tar -xf /usr/local/hive/packaging/target/apache-hive-${HIVE_VERSION}-bin.tar.gz -C /usr/local/hive-dist

# 设定Hive环境信息
ENV HIVE_HOME /usr/local/hive-dist/apache-hive-${HIVE_VERSION}-bin
ENV HIVE_CONF $HIVE_HOME/conf
ENV PATH $HIVE_HOME/bin:$PATH

# 添加Postgres JDBC连接包
RUN ln -s /usr/share/java/postgresql-jdbc4.jar $HIVE_HOME/lib/postgresql-jdbc4.jar

# to avoid psql asking password, set PGPASSWORD
ENV PGPASSWORD hive

# 初始化Hive 元数据库
RUN /etc/init.d/postgresql start &&\
	cd $HIVE_HOME/scripts/metastore/upgrade/postgres/ &&\
 	psql -h localhost -U hive -d metastore -f hive-schema-0.13.0.postgres.sql

# 复制配置文件、SQL以及数据文件
RUN mkdir /opt/files
ADD hive-site.xml /opt/files/
ADD hive-log4j.properties /opt/files/
ADD hive-site.xml $HIVE_CONF/hive-site.xml
ADD hive-log4j.properties $HIVE_CONF/hive-log4j.properties
ADD store_sales.* /opt/files/
ADD datagen.py /opt/files/

# 为Hive 启动配置设定权限
ADD hive-bootstrap.sh /etc/hive-bootstrap.sh
RUN chown root:root /etc/hive-bootstrap.sh
RUN chmod 700 /etc/hive-bootstrap.sh

# To overcome the bug in AUFS that denies postgres permission to read /etc/ssl/private/ssl-cert-snakeoil.key file.
# https://github.com/Painted-Fox/docker-postgresql/issues/30
# https://github.com/docker/docker/issues/783
# To avoid this issue lets disable ssl in postgres.conf. If we really need ssl to encrypt postgres connections we have to fix permissions to /etc/ssl/private directory everytime until AUFS fixes the issue
ENV POSTGRESQL_MAIN /var/lib/postgresql/9.3/main/
ENV POSTGRESQL_CONFIG_FILE $POSTGRESQL_MAIN/postgresql.conf
ENV POSTGRESQL_BIN /usr/lib/postgresql/9.3/bin/postgres
ADD postgresql.conf $POSTGRESQL_MAIN
RUN chown postgres:postgres $POSTGRESQL_CONFIG_FILE





