#!/bin/sh
export JAVA_HOME=/zsoft/jdk8/bin/
appHome=`pwd`
appJar="powerjob-worker-shapan-3.0.0.jar"
appCP=$appHome/$appJar
appShell=$JAVA_HOME"java -jar $appCP"
echo $appShell
echo 先终止已运行的“调度代理程序”
source ./stopWorker.sh
nohup $appShell &
echo “调度代理程序”已经启动