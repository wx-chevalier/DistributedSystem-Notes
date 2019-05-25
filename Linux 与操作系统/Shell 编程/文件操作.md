# 文件系统

# 文件判断

```sh
#!/bin/sh

FILENAME=
echo “Input file name：”
read FILENAME
if [ -c "$FILENAME" ]
then
cp $FILENAME /dev
fi
```

```sh
if [ -d "$LINK_OR_DIR" ]; then
  if [ -L "$LINK_OR_DIR" ]; then
    # It is a symlink!
    # Symbolic link specific commands go here.
    rm "$LINK_OR_DIR"
  else
    # It's a directory!
    # Directory command goes here.
    rmdir "$LINK_OR_DIR"
  fi
fi
```

# 文件遍历
