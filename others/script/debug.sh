#!/bin/sh
# 一键部署脚本，请勿挪动脚本
cd `dirname $0`/../.. || exit
echo "================== 构建 jar =================="
mvn clean package -DskipTests -Pdev -e -U
echo "================== 拷贝 jar =================="
/bin/cp -rf powerjob-server/target/*.jar others/powerjob-server.jar
ls -l others/powerjob-server.jar
echo "================== debug 模式启动 =================="
nohup java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar others/powerjob-server.jar > powerjob-server.log &
sleep 100
tail --pid=$$ -f -n 1000 others/powerjob-server.log
