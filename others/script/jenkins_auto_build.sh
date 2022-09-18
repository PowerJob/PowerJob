#!/bin/bash
cd `dirname $0`/../.. || exit
echo "================== 构建 jar =================="
mvn clean package -Pdev -DskipTests -e
echo "================== 拷贝 jar =================="
/bin/cp -rf powerjob-server/powerjob-server-starter/target/*.jar powerjob-server/docker/powerjob-server.jar
echo "================== 关闭老应用 =================="
docker stop powerjob-server
docker stop powerjob-worker-samples
docker stop powerjob-worker-samples2
echo "================== 删除老容器 =================="
docker container rm powerjob-server
docker container rm powerjob-worker-samples
docker container rm powerjob-worker-samples2
echo "================== 删除旧镜像 =================="
docker rmi -f tjqq/powerjob-server:latest
docker rmi -f tjqq/powerjob-worker-samples:latest
echo "================== 构建 powerjob-server 镜像 =================="
docker build -t tjqq/powerjob-server:latest powerjob-server/docker/. || exit
echo "================== 构建 powerjob-worker-samples 镜像 =================="
docker build -t tjqq/powerjob-worker-samples:latest powerjob-worker-samples/. || exit
echo "================== 准备启动 powerjob-server =================="
docker run -d \
       --restart=always \
       --name powerjob-server \
       -p 7700:7700 -p 10086:10086 -p 5001:5005 -p 10001:10000 \
       -e JVMOPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=10000 -Dcom.sun.management.jmxremote.rmi.port=10000 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false" \
       -e PARAMS="--oms.swagger.enable=true --spring.profiles.active=product --spring.datasource.core.jdbc-url=jdbc:mysql://remotehost:3306/powerjob-product?useUnicode=true&characterEncoding=UTF-8 --oms.mongodb.enable=false --spring.data.mongodb.uri=mongodb://remotehost:27017/powerjob-product" \
       -v ~/docker/powerjob-server:/root/powerjob/server -v ~/.m2:/root/.m2 \
       tjqq/powerjob-server:latest
sleep 60
echo "================== 准备启动 powerjob-agent =================="
serverIP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' powerjob-server)
serverAddress="$serverIP:7700"
echo "使用的Server地址：$serverAddress"

docker run -d \
       --restart=always \
       --name powerjob-worker-samples \
       -p 27777:27777 -p 5002:5005 -p 10002:10000 \
       -e JVMOPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=10000 -Dcom.sun.management.jmxremote.rmi.port=10000 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false" \
       -e PARAMS="--powerjob.worker.server-address=$serverAddress" \
       -v ~/docker/powerjob-worker-samples:/root \
       tjqq/powerjob-worker-samples:latest

docker run -d \
       --restart=always \
       --name powerjob-worker-samples2 \
       -p 27778:27777 -p 5003:5005 -p 10003:10000 \
       -e JVMOPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=10000 -Dcom.sun.management.jmxremote.rmi.port=10000 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false" \
       -e PARAMS="--powerjob.worker.server-address=$serverAddress" \
       -v ~/docker/powerjob-worker-samples2:/root \
       tjqq/powerjob-worker-samples:latest

