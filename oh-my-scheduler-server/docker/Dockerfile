# 基础镜像
FROM openjdk:8
# 维护者
MAINTAINER tengjiqi@gmail.com
# 下载并安装 maven
RUN curl -O https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
RUN tar -zxvf apache-maven-3.6.3-bin.tar.gz && mv apache-maven-3.6.3 /opt/oms-maven && rm -rf apache-maven-3.6.3-bin.tar.gz
# 替换 maven 配置文件
RUN rm -rf /opt/oms-maven/conf/settings.xml
COPY settings.xml /opt/oms-maven/conf/settings.xml
# 设置 maven 环境变量（maven invoker 读取该变量调用 maven）
ENV M2_HOME=/opt/oms-maven

# 设置其他环境变量
ENV APP_NAME=oh-my-scheduler-server
# 传递 SpringBoot 启动参数
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
ENTRYPOINT ["sh","-c","java -jar /oms-server.jar $PARAMS"]
