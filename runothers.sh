#!/usr/bin/env bash
COLOR=edab38 REDIS_RECORDS_KEY="bsg-the-plan" SERVICE_NAME="BaseStar05" PORT=8085 SERVICE_PORT=8085 java  -jar target/basestar-1.0-SNAPSHOT-fat.jar&
COLOR=9e37dd REDIS_RECORDS_KEY="bsg-the-plan" SERVICE_NAME="BaseStar06" PORT=8086 SERVICE_PORT=8086 java  -jar target/basestar-1.0-SNAPSHOT-fat.jar&
COLOR=3dedea REDIS_RECORDS_KEY="bsg-the-plan" SERVICE_NAME="BaseStar07" PORT=8087 SERVICE_PORT=8087 java  -jar target/basestar-1.0-SNAPSHOT-fat.jar&

