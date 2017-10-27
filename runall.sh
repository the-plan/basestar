#!/usr/bin/env bash
COLOR=FF5733 REDIS_RECORDS_KEY="bsg-the-plan" SERVICE_NAME="BaseStar01" PORT=8081 SERVICE_PORT=8081 java  -jar target/basestar-1.0-SNAPSHOT-fat.jar&
COLOR=339FFF REDIS_RECORDS_KEY="bsg-the-plan" SERVICE_NAME="BaseStar02" PORT=8082 SERVICE_PORT=8082 java  -jar target/basestar-1.0-SNAPSHOT-fat.jar&
COLOR=46FF33 REDIS_RECORDS_KEY="bsg-the-plan" SERVICE_NAME="BaseStar03" PORT=8083 SERVICE_PORT=8083 java  -jar target/basestar-1.0-SNAPSHOT-fat.jar&
COLOR=FF33F0 REDIS_RECORDS_KEY="bsg-the-plan" SERVICE_NAME="BaseStar04" PORT=8084 SERVICE_PORT=8084 java  -jar target/basestar-1.0-SNAPSHOT-fat.jar&

