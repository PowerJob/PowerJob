# 基础镜像（支持 amd64 & arm64），based on Ubuntu 18.04.4 LTS
#FROM harbor-qzbeta.mail.netease.com/library/java/serverjre:1.8.0_241-b07
#FROM harbor-qzbeta.mail.netease.com/library/java/spring-napm-base:1.1.1
FROM adoptopenjdk:8-jdk-hotspot
MAINTAINER oubaodian@corp.netease.com

# 设置时区
ENV TZ=Asia/Shanghai

# 设置其他环境变量
ENV APP_NAME=powerjob-server

ARG ARG_PARAMS=""
ARG ARG_JVM_OPTIONS="-Xms4g -Xmx4g"
ARG ARG_DEPLOY_ENV="test"

# 传递 SpringBoot 启动参数 和 JVM参数
ENV PARAMS=$ARG_PARAMS \
JVM_OPTIONS=$ARG_JVM_OPTIONS \
DEPLOY_ENV=$ARG_DEPLOY_ENV \
LOG_PATH="/home/logs"

# 将应用 jar 包拷入 docker
COPY ./powerjob-server/powerjob-server-starter/target/powerjob-server.jar /powerjob-server.jar
# 暴露端口（HTTP + AKKA + VertX）
EXPOSE 7700 10086 10010 1101
# 创建 docker 文件目录（盲猜这是用户目录）
RUN mkdir -p /home/logs
# 启动应用
ENTRYPOINT ["sh","-c","java -Dspring.profiles.active=$DEPLOY_ENV $JVM_OPTIONS -jar /powerjob-server.jar $PARAMS"]
