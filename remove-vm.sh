#!/usr/bin/env bash

rootname=basestar

for i in `seq 1 5`;
do
  echo "deleting $rootname$i application..."
  clever delete --alias $rootname$i --yes
done