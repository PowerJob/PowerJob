# 基础镜像（支持 amd64 & arm64），based on Ubuntu 18.04.4 LTS
# todo 切换这边的基础镜像
# harbor-qzbeta.mail.netease.com/library/java/jdk
FROM adoptopenjdk:8-jdk-hotspot
# 维护者
MAINTAINER oubaodian@corp.netease.com
# 下载并安装 maven ，其实这个步骤可有可无，暂时不需要这个功能点，先留着吧
RUN curl -O https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
RUN tar -zxvf apache-maven-3.6.3-bin.tar.gz && mv apache-maven-3.6.3 /opt/powerjob-maven && rm -rf apache-maven-3.6.3-bin.tar.gz
# 替换 maven 配置文件
RUN rm -rf /opt/powerjob-maven/conf/settings.xml
COPY /powerjob-server/docker/settings.xml /opt/powerjob-maven/conf/settings.xml
# 设置 maven 环境变量（maven invoker 读取该变量调用 maven）
ENV M2_HOME=/opt/powerjob-maven

# 设置时区
ENV TZ=Asia/Shanghai

# 设置其他环境变量
ENV APP_NAME=powerjob-server
# 传递 SpringBoot 启动参数 和 JVM参数
ENV PARAMS=""
ENV JVM_OPTIONS=""
ENV DEPLOY_ENV="test"
ENV LOG_PATH="/home/logs"

# 将应用 jar 包拷入 docker
COPY ./powerjob-server/powerjob-server-starter/target/powerjob-server.jar /powerjob-server.jar
# 暴露端口（HTTP + AKKA + VertX）
EXPOSE 7700 10086 10010
# 创建 docker 文件目录（盲猜这是用户目录）
RUN mkdir -p /root/powerjob-server
# 挂载数据卷，将文件直接输出到宿主机（注意，此处挂载的是匿名卷，即在宿主机位置随机）
VOLUME /root/powerjob
# 启动应用
ENTRYPOINT ["sh","-c","java -dSpring.profiles.active=$DEPLOY_ENV $JVM_OPTIONS -jar /powerjob-server.jar $PARAMS"]
