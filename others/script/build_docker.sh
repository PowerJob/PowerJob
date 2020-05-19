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
/bin/cp -rf oh-my-scheduler-server/target/*.jar others/oms-server.jar
ls -l others/oms-server.jar
echo "================== 构建应用镜像 =================="
docker build -t tjqq/oms-server:$version others/. || exit
echo "================== （关闭老应用）括号代表非必须，只是顺便运行下新版本进行测试 =================="
docker stop oms-server
echo "================== （删除老容器） =================="
docker container rm oms-server
