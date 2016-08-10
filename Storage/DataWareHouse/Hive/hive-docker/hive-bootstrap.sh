#!/bin/bash

echo "Starting postgresql server..."
sudo -u postgres $POSTGRESQL_BIN --config-file=$POSTGRESQL_CONFIG_FILE &


#start hadoop bootstrap script
/etc/bootstrap.sh

# start hive metastore server
$HIVE_HOME/bin/hive --service metastore &

sleep 5

# start hive server
$HIVE_HOME/bin/hive --service hiveserver2 &
