#!/bin/bash
# -p：允许后面跟一个字符串作为提示 -r：保证读入的是原始内容，不会发生任何转义
read -r -p "请输入Dockedr镜像版本:" version
echo "即将构建的 server 镜像：oms-server:$version"
echo "即将构建的 agent 镜像：oms-agent:$version"
read -r -p "任意键继续:"

# 一键部署脚本，请勿挪动脚本
cd `dirname $0`/../.. || exit

read -r -p "是否进行maven构建（y/n）:" needmvn
if [ "$needmvn" = "y" ] || [  "$needmvn" = "Y" ]; then
  echo "================== 构建 jar =================="
  mvn clean package -DskipTests -Pdev -U -e
  echo "================== 拷贝 jar =================="
  /bin/cp -rf oh-my-scheduler-server/target/*.jar oh-my-scheduler-server/oms-server.jar
  /bin/cp -rf oh-my-scheduler-worker-agent/target/*.jar oh-my-scheduler-worker-agent/oms-agent.jar
  ls -l oh-my-scheduler-server/oms-server.jar
  ls -l oh-my-scheduler-worker-agent/oms-agent.jar
fi

echo "================== 关闭老应用 =================="
docker stop oms-server
docker stop oms-agent
echo "================== 删除老容器 =================="
docker container rm oms-server
docker container rm oms-agent
echo "================== 删除旧镜像 =================="
docker rmi -f tjqq/oms-server:$version
docker rmi -f tjqq/oms-agent:$version
echo "================== 构建应用镜像 =================="
docker build -t tjqq/oms-server:$version oh-my-scheduler-server/. || exit
docker build -t tjqq/oms-agent:$version oh-my-scheduler-worker-agent/. || exit
echo "================== 准备启动应用 =================="

read -r -p "是否正式发布该镜像（y/n）:" needrelease
if [ "$needrelease" = "y" ] || [  "$needrelease" = "Y" ]; then
  read -r -p "三思！请确保当前处于已发布的Master分支！（y/n）:" needrelease
  if [ "$needrelease" = "y" ] || [  "$needrelease" = "Y" ]; then
    echo "================== 正在推送 server 镜像到中央仓库 =================="
    docker push tjqq/oms-server:$version
    echo "================== 正在推送 agent 镜像到中央仓库 =================="
    docker push tjqq/oms-agent:$version
  fi
fi

read -r -p "是否启动 server & agent（y/n）:" startup
if [ "$startup" = "y" ] || [  "$startup" = "Y" ]; then
  # 启动应用（端口映射、数据路径挂载）
  ## -d：后台运行
  ## -p：指定端口映射，容器端口：宿主机端口
  ## --name：指定容器名称
  ## -v（--volume）：挂载目录，宿主机目录：docker内目录，写入docker内路径的数据会被直接写到宿主机上，常用于日志文件
  ## -net=host：容器和宿主机共享网络（容器直接使用宿主机IP，性能最好，但网络隔离较差）
  docker run -d -e PARAMS="--spring.profiles.active=product" -p 7700:7700 -p 10086:10086 -p 27777:27777 --name oms-server -v ~/docker/oms-server:/root/oms-server tjqq/oms-server:$version
  sleep 1
  tail --pid=$$ -f -n 1000 ~/docker/oms-server/application.log

  docker run -d -e PARAMS="--app oms-agent-test"
fi