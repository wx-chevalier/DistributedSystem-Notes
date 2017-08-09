#!/bin/sh

docker tag username/caddy localhost:5000/username/caddy
docker push localhost:5000/username/caddy
