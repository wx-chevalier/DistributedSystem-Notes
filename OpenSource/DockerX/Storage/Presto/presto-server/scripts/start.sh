#!/bin/sh

CONTAINER_NAME=data-presto-server
PRESTO_VERSION=0.179
REGISTRY=10.196.108.176:5000

if docker inspect $CONTAINER_NAME 1>/dev/null 2>/dev/null; then
  echo "Container with name $CONTAINER_NAME already exists"
else
  echo "Creating presto data volume of version $PRESTO_VERSION"
  docker create \
    -v /presto/server \
    --name $CONTAINER_NAME \
    $REGISTRY/username/presto-server:$PRESTO_VERSION
fi
