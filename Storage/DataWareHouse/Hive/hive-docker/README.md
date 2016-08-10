Docker image to run Hive
===============================

## Current Version
* Apache Hive (inmobi trunk version)
* Apache Hadoop 2.5.0
* PostgreSQL 9.3 (Hive metastore backend)

## Pull the image

docker pull inmobi/docker-hive

## Run the image

docker run -itP inmobi/docker-hive /etc/hive-bootstrap.sh -bash
