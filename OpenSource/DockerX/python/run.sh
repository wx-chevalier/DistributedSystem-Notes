#!/bin/sh

# 下载应用数据
rm -rf app.tar.gz
curl --user UserName:CustomPassword $APP >> app.tar.gz

# 解压
tar -xvzf app.tar.gz

# 安装必须的依赖
pip install -r requirements.txt

# 启动当前应用
python $ENTRY