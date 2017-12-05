#!/bin/sh

# 删除并且下载应用，这里额外需要删除 package-lock.json
rm -rf ./*
curl --user UserName:CustomPassword $APP >> app.tar.gz

# 解压
tar -xvzf app.tar.gz

# 执行依赖安装，这里仅安装运行时依赖
npm install --only=production

# 下载依赖并且启动
# 这里为了兼容 egg.js，优先判断是否存在 launcher.sh，如果不存在则直接启动 pm2-docker
if [ ! -d "./launcher.sh"]; then 
    chmod +x ./launcher.sh
    ./launcher.sh
else
    pm2-docker start --env production $ENTRY
fi 
