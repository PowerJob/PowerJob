# 基础镜像
FROM openjdk:8
# 维护者
MAINTAINER tengjiqi@gmail.com
# 设置环境变量
ENV APP_NAME=oh-my-scheduler-server
# 传递SpringBoot 启动参数
ENV PARAMS=""
# 将应用 jar 包拷入 docker
COPY oms-server.jar /oms-server.jar
# 暴露端口（HTTP + AKKA）
EXPOSE 7700 10086
# 创建 docker 文件目录（盲猜这是用户目录）
RUN mkdir -p /root/oms-server
# 挂载数据卷，将文件直接输出到宿主机（注意，此处挂载的是匿名卷，即在宿主机位置随机）
VOLUME /root/oms-server
# 启动应用
ENTRYPOINT ["sh","-c","java $PARAMS -jar /oms-server.jar"]

