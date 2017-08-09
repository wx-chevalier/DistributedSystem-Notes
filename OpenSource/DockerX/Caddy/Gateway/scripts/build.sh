#!/bin/sh -eu

ROOT=`dirname "$0"`/..
cd $ROOT

docker build -t username/caddy .
