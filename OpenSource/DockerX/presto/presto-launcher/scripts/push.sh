#!/bin/sh

IMAGE_NAME=username/presto-launcher

docker tag $IMAGE_NAME localhost:5000/$IMAGE_NAME
docker push localhost:5000/$IMAGE_NAME
