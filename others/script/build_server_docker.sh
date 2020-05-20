#!/bin/bash
# -p：允许后面跟一个字符串作为提示 -r：保证读入的是原始内容，不会发生任何转义
read -r -p "请输入Dockedr镜像版本:" version
echo "即将构建的Docker镜像：oms-server:$version"
read -r -p "任意键继续:"

# 一键部署脚本，请勿挪动脚本
cd `dirname $0`/../.. || exit
echo "================== 构建 jar =================="
mvn clean package -DskipTests -Pdev -U -e
echo "================== 拷贝 jar =================="
/bin/cp -rf oh-my-scheduler-server/target/*.jar oh-my-scheduler-server/oms-server.jar
ls -l oh-my-scheduler-server/oms-server.jar
echo "================== 关闭老应用 =================="
docker stop oms-server
echo "================== 删除老容器 =================="
docker container rm oms-server
echo "================== 删除旧镜像 =================="
docker rmi -f tjqq/oms-server:$version
echo "================== 构建应用镜像 =================="
docker build -t tjqq/oms-server:$version oh-my-scheduler-server/. || exit
echo "================== 准备启动应用 =================="
# 启动应用（端口映射、数据路径挂载）
## -d：后台运行
## -p：指定端口映射，容器端口：宿主机端口
## --name：指定容器名称
## -v（--volume）：挂载目录，宿主机目录：docker内目录，写入docker内路径的数据会被直接写到宿主机上，常用于日志文件
## -net=host：容器和宿主机共享网络（容器直接使用宿主机IP，性能最好，但网络隔离较差）
docker run -d -e PARAMS="--spring.profiles.active=product" -p 7700:7700 -p 10086:10086 -p 27777:27777 --name oms-server -v ~/docker/oms-server:/root/oms-server tjqq/oms-server:$version
sleep 1
tail --pid=$$ -f -n 1000 ~/docker/oms-server/application.log