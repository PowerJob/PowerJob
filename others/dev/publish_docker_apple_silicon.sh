#!/bin/bash
echo "A docker image release script for the Apple Silicon device."
# -p：允许后面跟一个字符串作为提示 -r：保证读入的是原始内容，不会发生任何转义
read -r -p "请输入Dockedr镜像版本:" version
echo "即将构建的 server 镜像：powerjob-server:$version"
echo "即将构建的 agent 镜像：powerjob-agent:$version"
read -r -p "任意键继续:"

# 一键部署脚本，请勿挪动脚本
cd `dirname $0`/../.. || exit

read -r -p "是否进行maven构建（y/n）:" needmvn
if [ "$needmvn" = "y" ] || [  "$needmvn" = "Y" ]; then
  echo "================== 构建 jar =================="
  # mvn clean package -Pdev -DskipTests -U -e -pl powerjob-server,powerjob-worker-agent -am
  # -U：强制检查snapshot库 -pl：指定需要构建的模块，多模块逗号分割 -am：同时构建依赖模块，一般与pl连用 -Pxxx：指定使用的配置文件
  mvn clean package -Pdev -DskipTests -U -e
  echo "================== 拷贝 jar =================="
  /bin/cp -rf powerjob-server/powerjob-server-starter/target/*.jar powerjob-server/docker/powerjob-server.jar
  /bin/cp -rf powerjob-worker-agent/target/*.jar powerjob-worker-agent/powerjob-agent.jar
  ls -l powerjob-server/docker/powerjob-server.jar
  ls -l powerjob-worker-agent/powerjob-agent.jar
fi

echo "================== 关闭老应用 =================="
docker stop powerjob-server
docker stop powerjob-agent
docker stop powerjob-agent2
echo "================== 删除老容器 =================="
docker container rm powerjob-server
docker container rm powerjob-agent
docker container rm powerjob-agent2
read -r -p "是否构建并发布镜像（y/n）:" rebuild
if [ "$rebuild" = "y" ] || [  "$rebuild" = "Y" ]; then
  echo "================== 删除旧镜像 =================="
  docker rmi -f tjqq/powerjob-server:$version
  docker rmi -f powerjob/powerjob-server:$version
  docker rmi -f tjqq/powerjob-agent:$version
  docker rmi -f powerjob/powerjob-agent:$version
  docker rmi -f powerjob/powerjob-mysql:$version
  docker rmi -f powerjob/powerjob-worker-samples:$version
  echo "================== 构建 powerjob-server 镜像(tjqq) =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag tjqq/powerjob-server:$version powerjob-server/docker/. --push || exit
  echo "================== 构建 powerjob-server 镜像(powerjob) =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag powerjob/powerjob-server:$version powerjob-server/docker/. --push || exit
  echo "================== 构建 powerjob-agent 镜像(tjqq) =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag tjqq/powerjob-agent:$version powerjob-worker-agent/. --push|| exit
  echo "================== 构建 powerjob-agent 镜像(powerjob) =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag powerjob/powerjob-agent:$version powerjob-worker-agent/. --push|| exit
  echo "================== 构建 powerjob-mysql 镜像 =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag powerjob/powerjob-mysql:$version others/. --push|| exit
  echo "================== 构建 powerjob-worker-samples 镜像 =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag powerjob/powerjob-worker-samples:$version powerjob-worker-samples/. --push|| exit
fi

read -r -p "是否推送LATEST（y/n）:" push_latest
if [ "$push_latest" = "y" ] || [  "$push_latest" = "Y" ]; then

  echo "==================  powerjob-server LATEST (tjqq) =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag tjqq/powerjob-server:latest powerjob-server/docker/. --push || exit
  echo "==================  powerjob-server LATEST (powerjob) =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag powerjob/powerjob-server:latest powerjob-server/docker/. --push || exit
  echo "==================  powerjob-agent LATEST (tjqq) =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag tjqq/powerjob-agent:latest powerjob-worker-agent/. --push|| exit
  echo "==================  powerjob-agent LATEST (powerjob) =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag powerjob/powerjob-agent:latest powerjob-worker-agent/. --push|| exit
  echo "==================  powerjob-mysql LATEST =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag powerjob/powerjob-mysql:latest others/. --push|| exit
  echo "==================  powerjob-worker-samples LATEST =================="
  docker buildx build --platform=linux/amd64,linux/arm64 --tag powerjob/powerjob-worker-samples:latest powerjob-worker-samples/. --push|| exit
fi