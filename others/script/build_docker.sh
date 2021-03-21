#!/bin/bash
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
read -r -p "是否重新构建镜像（y/n）:" rebuild
if [ "$rebuild" = "y" ] || [  "$rebuild" = "Y" ]; then
  echo "================== 删除旧镜像 =================="
  docker rmi -f tjqq/powerjob-server:$version
  docker rmi -f tjqq/powerjob-agent:$version
  echo "================== 构建 powerjob-server 镜像 =================="
  docker build -t tjqq/powerjob-server:$version powerjob-server/docker/. || exit
  echo "================== 构建 powerjob-agent 镜像 =================="
  docker build -t tjqq/powerjob-agent:$version powerjob-worker-agent/. || exit

  read -r -p "是否正式发布该镜像（y/n）:" needrelease
  if [ "$needrelease" = "y" ] || [  "$needrelease" = "Y" ]; then
    read -r -p "三思！请确保当前处于已发布的Master分支！（y/n）:" needrelease
    if [ "$needrelease" = "y" ] || [  "$needrelease" = "Y" ]; then
      echo "================== 正在推送 server 镜像到中央仓库 =================="
      docker push tjqq/powerjob-server:$version
      echo "================== 正在推送 agent 镜像到中央仓库 =================="
      docker push tjqq/powerjob-agent:$version
    fi
  fi
fi


read -r -p "是否启动 server & agent（y/n）:" startup
if [ "$startup" = "y" ] || [  "$startup" = "Y" ]; then
  # 启动应用（端口映射、数据路径挂载）
  ## -d：后台运行
  ## -p：指定端口映射，主机端口:容器端口
  ## --name：指定容器名称
  ## -v（--volume）：挂载目录，宿主机目录：docker内目录，写入docker内路径的数据会被直接写到宿主机上，常用于日志文件
  ## --net=host：容器和宿主机共享网络（容器直接使用宿主机IP，性能最好，但网络隔离较差）
  echo "================== 准备启动 powerjob-server =================="
  docker run -d \
         --name powerjob-server \
         -p 7700:7700 -p 10086:10086 -p 5001:5005 -p 10001:10000 \
         -e JVMOPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=10000 -Dcom.sun.management.jmxremote.rmi.port=10000 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false" \
         -e PARAMS="--spring.profiles.active=pre" \
         -e TZ="Asia/Shanghai" \
         -v ~/docker/powerjob-server:/root/powerjob-server -v ~/.m2:/root/.m2 \
         tjqq/powerjob-server:$version
  sleep 1
#  tail -f -n 1000 ~/docker/powerjob-server/logs/powerjob-server-application.log

  sleep 30
  echo "================== 准备启动 powerjob-client =================="
  serverIP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' powerjob-server)
  serverAddress="$serverIP:7700"
  echo "使用的Server地址：$serverAddress"
  docker run -d \
         --name powerjob-agent \
         -p 27777:27777 -p 5002:5005 -p 10002:10000 \
         -e JVMOPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=10000 -Dcom.sun.management.jmxremote.rmi.port=10000 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false" \
         -e PARAMS="--app powerjob-agent-test --server $serverAddress" \
         -v ~/docker/powerjob-agent:/root \
         tjqq/powerjob-agent:$version

  docker run -d \
         --name powerjob-agent2 \
         -p 27778:27777 -p 5003:5005 -p 10003:10000 \
         -e JVMOPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" \
         -e PARAMS="--app powerjob-agent-test --server $serverAddress" \
         -v ~/docker/powerjob-agent2:/root \
         tjqq/powerjob-agent:$version

  tail -f -n 100 ~/docker/powerjob-agent/powerjob/logs/powerjob-agent-application.log
fi