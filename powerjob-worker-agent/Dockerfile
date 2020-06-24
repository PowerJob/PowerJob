# 为了便于使用 arthas，agent 也使用 jdk 而不是 jre
FROM adoptopenjdk:8-jdk-hotspot
MAINTAINER tengjiqi@gmail.com
# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV APP_NAME=powerjob-worker-agent
ENV PARAMS=""

COPY powerjob-agent.jar /powerjob-agent.jar
# 暴露端口（AKKA-Client）
EXPOSE 27777
# 挂载数据卷，将文件直接输出到宿主机（注意，此处挂载的是匿名卷，即在宿主机位置随机）
VOLUME /root
# 启动应用
ENTRYPOINT ["sh","-c","java -jar /powerjob-agent.jar $PARAMS"]