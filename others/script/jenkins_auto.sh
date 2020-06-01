#!/bin/bash
cd `dirname $0`/../.. || exit
echo "================== 构建 jar =================="
mvn clean package -DskipTests -Pdev -U -e
echo "================== 拷贝 jar =================="
/bin/cp -rf oh-my-scheduler-server/target/*.jar oh-my-scheduler-server/oms-server.jar
/bin/cp -rf oh-my-scheduler-worker-agent/target/*.jar oh-my-scheduler-worker-agent/oms-agent.jar
ls -l oh-my-scheduler-server/oms-server.jar
ls -l oh-my-scheduler-worker-agent/oms-agent.jar
echo "================== 关闭老应用 =================="
docker stop oms-server
docker stop oms-agent
docker stop oms-agent2
echo "================== 删除老容器 =================="
docker container rm oms-server
docker container rm oms-agent
docker container rm oms-agent2
echo "================== 删除旧镜像 =================="
docker rmi -f tjqq/oms-server:latest
docker rmi -f tjqq/oms-agent:latest
echo "================== 构建 oms-server 镜像 =================="
docker build -t tjqq/oms-server:latest oh-my-scheduler-server/. || exit
echo "================== 构建 oms-agent 镜像 =================="
docker build -t tjqq/oms-agent:latest oh-my-scheduler-worker-agent/. || exit
echo "================== 准备启动 oms-server =================="
docker run -d -e PARAMS="--spring.profiles.active=product" -p 7700:7700 -p 10086:10086 --name oms-server -v ~/docker/oms-server:/root/oms-server tjqq/oms-server:latest
sleep 60
echo "================== 准备启动 oms-client =================="
serverIP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' oms-server)
serverAddress="$serverIP:7700"
echo "使用的Server地址：$serverAddress"
docker run -d -e PARAMS="--app oms-agent-test --server $serverAddress" -p 27777:27777 --name oms-agent -v ~/docker/oms-agent:/root tjqq/oms-agent:latest
docker run -d -e PARAMS="--app oms-agent-test --server $serverAddress" -p 27778:27777 --name oms-agent2 -v ~/docker/oms-agent2:/root tjqq/oms-agent:latest

