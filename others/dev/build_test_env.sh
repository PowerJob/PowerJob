#!/bin/bash
# 构建 PowerJob 测试环境

echo "================== 关闭全部服务 =================="
docker-compose down
echo "================== 构建 jar =================="
cd `dirname $0`/../.. || exit
# mvn clean package -Pdev -DskipTests -U -e -pl powerjob-server,powerjob-worker-agent -am
# -U：强制检查snapshot库 -pl：指定需要构建的模块，多模块逗号分割 -am：同时构建依赖模块，一般与pl连用 -Pxxx：指定使用的配置文件
mvn clean package -Pdev -DskipTests
echo "================== 拷贝 jar =================="
/bin/cp -rf powerjob-server/powerjob-server-starter/target/*.jar powerjob-server/docker/powerjob-server.jar
/bin/cp -rf powerjob-worker-agent/target/*.jar powerjob-worker-agent/powerjob-agent.jar
ls -l powerjob-server/docker/powerjob-server.jar
ls -l powerjob-worker-agent/powerjob-agent.jar

cd others/dev
docker-compose build
docker-compose --compatibility up