#!/bin/sh
echo 查看“调度代理程序”的进程
ps aux|egrep 'java.*powerjob-worker-shapan-3.0.0.jar|PID'|grep -v grep
echo 终止“调度代理程序”的进程...
ps -ef|grep java.*powerjob-worker-shapan-3.0.0.jar|grep -v grep|awk '{print $2}'|xargs kill -9
echo 进程已终止