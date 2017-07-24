#!/usr/bin/env bash

rootname=basestar
addon=the-plan-b

for i in `seq 1 5`;
do
  echo "creating $rootname$i application..."
  clever create -t maven $rootname$i --org wey-yu --region par --alias $rootname$i --github the-plan/basestar;
  clever service link-addon $addon --alias $rootname$i;
  clever env set PORT 8080 --alias $rootname$i;
  clever env set SERVICE_PORT 80 --alias $rootname$i;
  clever env set SERVICE_NAME $rootname$i --alias $rootname$i;
  clever env set SERVICE_HOST $rootname$i.cleverapps.io --alias $rootname$i;
  clever env set REDIS_CHANNEL the-plan --alias $rootname$i;
  clever env set REDIS_RECORDS_KEY the-plan-ms --alias $rootname$i;
  clever domain add $rootname$i.cleverapps.io --alias $rootname$i;
  #clever scale --flavor S --alias $rootname$i;
  #clever restart --quiet true --alias $rootname$i;
done
