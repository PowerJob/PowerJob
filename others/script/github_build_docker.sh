#!/bin/bash

cd `dirname $0`/../.. || exit
echo "================== 构建 jar =================="
mvn clean package -Pdev -DskipTests -U -e -pl oh-my-scheduler-server,oh-my-scheduler-worker-agent -am
echo "================== 拷贝 jar =================="
/bin/cp -rf oh-my-scheduler-server/target/*.jar oh-my-scheduler-server/docker/oms-server.jar
/bin/cp -rf oh-my-scheduler-worker-agent/target/*.jar oh-my-scheduler-worker-agent/oms-agent.jar
ls -l oh-my-scheduler-server/docker/oms-server.jar
ls -l oh-my-scheduler-worker-agent/oms-agent.jar
echo "================== 构建 oms-server 镜像 =================="
docker build -t tjqq/oms-server:latest oh-my-scheduler-server/docker/. || exit
echo "================== 构建 oms-agent 镜像 =================="
docker build -t tjqq/oms-agent:latest oh-my-scheduler-worker-agent/. || exit