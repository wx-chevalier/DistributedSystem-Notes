#!/bin/sh

# 删除并且下载应用
rm -rf ./*
curl --user UserName:CustomPassword $APP >> app.tar.gz

# 解压到当前文件夹
tar -xvzf app.tar.gz

# 启动 Caddy
/usr/bin/caddy -http2=false --conf /etc/Caddyfile -port 80 --log stdout