# 为了便于使用 arthas，agent 也使用 jdk 而不是 jre
FROM adoptopenjdk:8-jdk-hotspot
MAINTAINER tengjiqi@gmail.com

ENV APP_NAME=powerjob-worker-agent
# 传递 SpringBoot 启动参数 和 JVM参数
ENV PARAMS=""
ENV JVMOPTIONS=""

# 默认安装 python2.7
RUN apt-get update && \
    apt-get install -y python \
    && apt-get clean \
    && apt-get autoclean \
    && rm -rf /var/lib/apt/lists/*
# 安装 wait-for-it 脚本
RUN curl -o wait-for-it.sh https://gitee.com/KFCFans/wait-for-it/raw/master/wait-for-it.sh
RUN chmod +x wait-for-it.sh
# 拷贝主文件
COPY powerjob-agent.jar /powerjob-agent.jar
# 暴露端口（AKKA-Client）
EXPOSE 27777
# 挂载数据卷，将文件直接输出到宿主机（注意，此处挂载的是匿名卷，即在宿主机位置随机）
VOLUME /root
# 启动应用
ENTRYPOINT ["sh","-c","java $JVMOPTIONS -jar /powerjob-agent.jar $PARAMS"]