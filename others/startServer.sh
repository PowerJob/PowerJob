#!/bin/sh
export JAVA_HOME=/zsoft/jdk8/bin/
appHome=`pwd`
appJar="powerjob-server-starter-4.0.1.jar"
appCP=$appHome/$appJar
appShell=$JAVA_HOME"java -jar $appCP --spring.profiles.active=product"
echo $appShell
echo 先终止已运行的“调度程序”
source ./stopServer.sh
nohup $appShell &
echo “调度程序”已经启动
