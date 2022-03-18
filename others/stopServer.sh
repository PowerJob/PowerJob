#!/bin/sh
echo 查看“调度程序”的进程
ps aux|egrep 'java.*powerjob-server-starter-4.0.1.jar|PID'|grep -v grep
echo 终止“调度程序”的进程...
ps -ef|grep java.*powerjob-server-starter-4.0.1.jar|grep -v grep|awk '{print $2}'|xargs kill -9
echo 进程已终止