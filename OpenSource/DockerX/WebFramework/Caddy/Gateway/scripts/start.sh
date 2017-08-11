#!/bin/sh

CONTAINER_NAME=CaddyGateway

echo "Using /var/data/infrastructure/deploy_uploaed for uploaded data's volume"
mkdir -p /var/data/infrastructure/deploy_uploaed

CONTAINER_ID=`docker -a ps -q -f name=$CONTAINER_NAME`
if [ ! -z $CONTAINER_ID ]; then
  echo "Container [name=$CONTAINER_NAME] exits, restart it ..."
  docker restart $CONTAINER_NAME
else
  # Caddy 证书存放在了 $HOME/.caddy 目录下
  echo "Starting container with name=$CONTAINER_NAME"
  docker run -d \
    --name $CONTAINER_NAME \
    -v $HOME/.caddy:/root/.caddy \
    -p 80:80 -p 443:443 \
    -v /var/data/infrastructure/deploy_uploaed:/opt/workspace/uploaded \
    username/caddy
fi
