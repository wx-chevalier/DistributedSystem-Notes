#!/bin/sh

REPO_URL=${CONFIG_REPO}
DRAGON_DOCKER_REPO=/opt/workspace/caddy
GATEWAY_CADDYFILE=$DRAGON_DOCKER_REPO/Caddy/Gateway/Caddyfile
GATEWAY_VHOSTS=$DRAGON_DOCKER_REPO/Caddy/Gateway/vhosts

# 下载代码
echo "Downloading configurations from $REPO_URL to $DRAGON_DOCKER_REPO"
git clone $REPO_URL $DRAGON_DOCKER_REPO

if [ ! -d $DRAGON_DOCKER_REPO ]; then
  echo "Failed downloading configurations from $REPO_URL, quitting"
  exit 1
fi

if [ -f $GATEWAY_CADDYFILE ]; then
  echo "Clearing /etc/Caddyfile"
  rm -rf /etc/Caddyfile
  echo "Copying $GATEWAY_CADDYFILE to /etc/Caddyfile"
  mv $GATEWAY_CADDYFILE /etc/Caddyfile
else
  echo "Caddyfile not found at $GATEWAY_CADDYFILE"
fi

if [ -d $GATEWAY_VHOSTS ]; then
  echo "Clearing /etc/vhosts"
  rm -rf /etc/vhosts
  echo "Copying $GATEWAY_VHOSTS to /etc/vhosts"
  mv $GATEWAY_VHOSTS /etc/vhosts
else
  echo "Vhosts not found at $GATEWAY_VHOSTS"
fi

echo "Clearing configuration repo $DRAGON_DOCKER_REPO"
rm -rf $DRAGON_DOCKER_REPO

echo "Preparing for start..."
echo "Creating /opt/workspace/uploaded for data uploaded from deploy.hostname.cn"
mkdir -p /opt/workspace/uploaded

# 启动 Caddy
echo "Starting caddy"
/usr/bin/caddy -http2=false --conf /etc/Caddyfile --log stdout