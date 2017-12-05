#!/bin/sh

if [[ -z "$APP" ]]; then
  echo "APP variable is not specified, you should set it to be the address of the app, quitting."
  exit 1
fi

# 删除并且下载应用
rm -rf app.tar.gz
curl --user UserName:CustomPassword $APP -o app.tar.gz

# 解压
tar -xvzf app.tar.gz

# 启动应用
if [ -f "app/launcher.sh" ]; then
  echo "Starting app using user specified launcher script..."
  sh app/launcher.sh
else
  if [[ -z "$ENTRY" ]]; then
    echo "Try starting app using entry, but ENTRY is not specified, quittting."
    exit 1
  fi
  /usr/bin/java -jar $ENTRY
fi