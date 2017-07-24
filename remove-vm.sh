#!/usr/bin/env bash

rootname=basestar

for i in `seq 1 3`;
do
  echo "deleting $rootname$i application..."
  clever delete --alias $rootname$i --yes
done