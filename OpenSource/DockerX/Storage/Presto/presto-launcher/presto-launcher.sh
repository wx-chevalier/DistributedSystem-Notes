# imported ENVs
# PRESTO_HOME, PLUGIN, CONFIG

TMP_DIR=/presto/tmp
BASE_URL=http://deploy.hostname.cn/dragondc/presto
CREDENTIAL=UserName:CustomPassword

NODE_ID=/presto/node_id
if [ -z "$NODE_ENVIRONMENT" ]; then
  NODE_ENVIRONMENT=production
fi
NODE_DATA_DIR=/presto/data

PLUGIN_URL=$BASE_URL/plugin/$PLUGIN.tar.gz
CONFIG_URL=$BASE_URL/config/$CONFIG.tar.gz

if [[ ! -z "$PLUGIN" ]]; then
  echo "Using plugin from $PLUGIN_URL"
fi

if [[ ! -z "$CONFIG" ]]; then
  echo "Using configuration from $CONFIG_URL"
else
  echo "No configuration specified, quitting"
  exit 1
fi

echo "Clearing tmp directory $TMP_DIR"
rm -rf $TMP_DIR 2> /dev/null
mkdir -p $TMP_DIR 2> /dev/null
echo "Entering directory $TMP_DIR"
cd $TMP_DIR

if [[ ! -z "$PLUGIN" ]]; then
  echo "Downloading plugin..."
  curl --user $CREDENTIAL $PLUGIN_URL -o plugin.tar.gz
  if tar xfz plugin.tar.gz 2> null; then
    echo "Copy plugins from $TMP_DIR/plugin to $PRESTO_HOME/plugin"
    mv $TMP_DIR/plugin/* $PRESTO_HOME/plugin
    rm -rf $TMP_DIR/* 2> /dev/null
  else
    echo "Plugin from $PLUGIN_URL corrupted, quitting."
    rm -rf $TMP_DIR 2> /dev/null
    exit 1
  fi
fi

if [ ! -f $NODE_ID ]; then
  printf "import uuid\nprint 'node.id=' + str(uuid.uuid4())\n" > node_id.py \
         && python node_id.py > $NODE_ID \
         && rm node_id.py
  echo "Generate node id: `cat $NODE_ID`"
else
  echo "Using previous generated node id: `cat $NODE_ID`"
fi

echo "Downloading configuration..."
curl --user $CREDENTIAL $CONFIG_URL -o config.tar.gz
if tar xfz config.tar.gz 2> null; then
  echo "Copy config from $TMP_DIR/config to $PRESTO_HOME/etc"
  rm -rf $PRESTO_HOME/etc 2> /dev/null
  mv $TMP_DIR/config $PRESTO_HOME/etc
  rm -rf $TMP_DIR/* 2> /dev/null
else
  echo "Config from $CONFIG_URL corrupted, quitting."  
  rm -rf $TMP_DIR 2> /dev/null
  exit 1
fi

echo "Setup node properties"
echo "node.environment=$NODE_ENVIRONMENT" > $PRESTO_HOME/etc/node.properties
cat $NODE_ID >> $PRESTO_HOME/etc/node.properties
echo "node.data-dir=$NODE_DATA_DIR" >> $PRESTO_HOME/etc/node.properties

# Starting presto server
/presto/server/bin/launcher "$@" && /bin/sh
