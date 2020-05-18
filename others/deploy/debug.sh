#!/bin/sh
# 一键部署脚本，请勿挪动脚本
cd `dirname $0`/../.. || exit
echo "================== 构建 jar =================="
mvn clean package -DskipTests -Pdev -e -U
echo "================== 拷贝 jar =================="
/bin/cp -rf oh-my-scheduler-server/target/*.jar others/deploy/oms-server.jar
ls -l others/deploy/oms-server.jar
echo "================== debug 模式启动 =================="
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar others/deploy/oms-server.jar
