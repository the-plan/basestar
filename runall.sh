#!/usr/bin/env bash
SERVICE_NAME="BaseStar01" PORT=8081 SERVICE_PORT=8081 java  -jar target/basestar-1.0-SNAPSHOT-fat.jar&
SERVICE_NAME="BaseStar02" PORT=8082 SERVICE_PORT=8082 java  -jar target/basestar-1.0-SNAPSHOT-fat.jar&
SERVICE_NAME="BaseStar03" PORT=8083 SERVICE_PORT=8083 java  -jar target/basestar-1.0-SNAPSHOT-fat.jar&

