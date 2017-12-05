#!/bin/sh -eu

IMAGE_NAME=username/presto-server:0.179

ROOT=`dirname "$0"`/..
cd $ROOT

docker build -t $IMAGE_NAME .
