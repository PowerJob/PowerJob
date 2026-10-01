# 基础镜像（支持 amd64 & arm64），based on Ubuntu 18.04.4 LTS
FROM adoptopenjdk:8-jdk-hotspot
# 维护者
MAINTAINER tengjiqi@gmail.com
# 下载并安装 maven
RUN curl -fsSL --retry 3 -o apache-maven-3.9.11-bin.tar.gz https://archive.apache.org/dist/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.tar.gz \
    && echo "bcfe4fe305c962ace56ac7b5fc7a08b87d5abd8b7e89027ab251069faebee516b0ded8961445d6d91ec1985dfe30f8153268843c89aa392733d1a3ec956c9978  apache-maven-3.9.11-bin.tar.gz" | sha512sum --check -
RUN tar -zxvf apache-maven-3.9.11-bin.tar.gz && mv apache-maven-3.9.11 /opt/powerjob-maven && rm -rf apache-maven-3.9.11-bin.tar.gz
# 替换 maven 配置文件
RUN rm -rf /opt/powerjob-maven/conf/settings.xml
COPY settings.xml /opt/powerjob-maven/conf/settings.xml
# 设置 maven 环境变量（maven invoker 读取该变量调用 maven）
ENV M2_HOME=/opt/powerjob-maven

# 设置时区
ENV TZ=Asia/Shanghai

# 设置其他环境变量
ENV APP_NAME=powerjob-server
# 传递 SpringBoot 启动参数 和 JVM参数
ENV PARAMS=""
ENV JVMOPTIONS=""
# 将应用 jar 包拷入 docker
COPY powerjob-server.jar /powerjob-server.jar
# 暴露端口（HTTP + AKKA + VertX）
EXPOSE 7700 10086 10010
# 创建 docker 文件目录（盲猜这是用户目录）
RUN mkdir -p /root/powerjob-server
# 挂载数据卷，将文件直接输出到宿主机（注意，此处挂载的是匿名卷，即在宿主机位置随机）
# VOLUME /root/powerjob
# 启动应用
ENTRYPOINT ["sh","-c","exec java $JVMOPTIONS -jar /powerjob-server.jar $PARAMS"]
