#!/bin/sh

CONTAINER_NAME=presto-server
REGISTRY=10.196.108.176:5000

PRESTO_DATA_VOLUME=data-presto-server
PRESTO_DATA=/var/data/presto

echo "Using $PRESTO_DATA for uploaded data's volume"
mkdir -p $PRESTO_DATA

if docker inspect $PRESTO_DATA_VOLUME 1>/dev/null 2>/dev/null; then
  # data volume exists
  echo "Presto data volume $PRESTO_DATA_VOLUME checked ok."
else
  echo "Data volume not found, you should start it first, checkout Storage/Presto/presto-server for details, quitting ..."
  exit 1
fi

if docker inspect $CONTAINER_NAME 1>/dev/null 2>/dev/null; then
  echo "Container [name=$CONTAINER_NAME] exits, restart it ..."
  docker restart $CONTAINER_NAME
else
  echo "Starting container with name=$CONTAINER_NAME"
  docker run -d -t \
    --name $CONTAINER_NAME \
    -v $PRESTO_DATA:/presto/data \
    --volumes-from $PRESTO_DATA_VOLUME \
    -p 7070:7070 \
    $@ \
    $REGISTRY/username/presto-launcher
fi
