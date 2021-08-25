# 基础镜像（支持 amd64 & arm64），based on Ubuntu 18.04.4 LTS
FROM adoptopenjdk:8-jdk-hotspot
MAINTAINER oubaodian@corp.netease.com

ARG ARG_PARAMS=""
ARG ARG_JVM_OPTIONS="-Xms1g -Xmx1g"
ARG ARG_DEPLOY_ENV="test"

# 设置时区
ENV TZ=Asia/Shanghai

# 设置其他环境变量
ENV APP_NAME=chronos-executor


# 传递 SpringBoot 启动参数 和 JVM参数
ENV PARAMS=$ARG_PARAMS \
JVM_OPTIONS=$ARG_JVM_OPTIONS \
DEPLOY_ENV=$ARG_DEPLOY_ENV \
LOG_PATH="/home/logs"

# 将应用 jar 包拷入 docker
COPY ./mail-chronos-executor/target/chronos-executor.jar /mail-chronos-executor.jar
EXPOSE 18087
# 创建 docker 文件目录（盲猜这是用户目录）
RUN mkdir -p /home/logs
# 启动应用
ENTRYPOINT ["sh","-c","java -Dspring.profiles.active=$DEPLOY_ENV $JVM_OPTIONS -jar /mail-chronos-executor.jar $PARAMS"]
