#!/bin/sh -eu

IMAGE_NAME=username/presto-launcher

ROOT=`dirname "$0"`/..
cd $ROOT

docker build -t $IMAGE_NAME .
