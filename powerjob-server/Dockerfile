FROM maven:3.6.1-jdk-8-alpine as build
WORKDIR /app

# cache as many dependencies as possible
# COPY pom.xml .
# COPY powerjob-server-remote/pom.xml powerjob-server-remote/pom.xml
# COPY powerjob-server-persistence/pom.xml powerjob-server-persistence/pom.xml
# COPY powerjob-server-common/pom.xml powerjob-server-common/pom.xml
# COPY powerjob-server-migrate/pom.xml powerjob-server-migrate/pom.xml
# COPY powerjob-server-starter/pom.xml powerjob-server-starter/pom.xml
# COPY powerjob-server-core/pom.xml powerjob-server-core/pom.xml
# COPY powerjob-server-extension/pom.xml powerjob-server-extension/pom.xml

# RUN --mount=type=cache,target=/root/.m2 mvn -DexcludeReactor=true -U dependency:resolve dependency:resolve-plugins

COPY pom.xml .
COPY powerjob-server-remote powerjob-server-remote
COPY powerjob-server-persistence powerjob-server-persistence
COPY powerjob-server-common powerjob-server-common
COPY powerjob-server-migrate powerjob-server-migrate
COPY powerjob-server-starter powerjob-server-starter
COPY powerjob-server-core powerjob-server-core
COPY powerjob-server-extension powerjob-server-extension

RUN mkdir -p /root/.m2
ADD .m2/settings.xml /root/.m2/settings.xml

RUN --mount=type=cache,target=/root/.m2 mvn -e -P macos-repo -B package -DskipTests=true
RUN mkdir -p powerjob-server-starter/target/exploded && \
    cd powerjob-server-starter/target/exploded && \
    jar -xf ../*.jar

FROM schnell18/zulu-java:alpine-jre17-0.1.1
LABEL MAINTAINER schnell18@gmail.com

ENV HOME=/powerjob
ENV M2_HOME=/powerjob/maven

# 下载并安装 maven
ARG MAVEN_VERSION="3.6.3"
ARG MAVEN_DOWNLOAD_URL=https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz

RUN mkdir -p $HOME && \
    curl -L --silent ${MAVEN_DOWNLOAD_URL} | tar -xzf - -C /powerjob && \
    mv /powerjob/apache-maven-${MAVEN_VERSION} /powerjob/maven
COPY docker/settings.xml /powerjob/maven/conf/settings.xml

ARG EXPLODED=/app/powerjob-server-starter/target/exploded
COPY --from=build ${EXPLODED}/BOOT-INF/lib /powerjob/lib
COPY --from=build ${EXPLODED}/META-INF /powerjob/classes/META-INF
COPY --from=build ${EXPLODED}/BOOT-INF/classes /powerjob/classes
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
# 设置其他环境变量
ENV APP_NAME=powerjob-server
# 暴露端口（HTTP + AKKA + VertX）
EXPOSE 7700 10086 10010
ENTRYPOINT ["/entrypoint.sh"]

