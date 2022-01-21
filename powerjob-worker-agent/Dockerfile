FROM maven:3.6.1-jdk-8-alpine as build
WORKDIR /app

COPY pom.xml .
COPY src src

RUN mkdir -p /root/.m2
ADD .m2/settings.xml /root/.m2/settings.xml

RUN --mount=type=cache,target=/root/.m2 mvn -e -P macos-repo -B package -DskipTests=true
RUN mkdir -p target/exploded && \
    cd target/exploded && \
    jar -xf ../*.jar

FROM schnell18/zulu-java:alpine-jre17-0.1.1
LABEL MAINTAINER schnell18@gmail.com

ENV HOME=/powerjob

ARG EXPLODED=/app/target/exploded
COPY --from=build ${EXPLODED}/BOOT-INF/lib /powerjob/lib
COPY --from=build ${EXPLODED}/META-INF /powerjob/classes/META-INF
COPY --from=build ${EXPLODED}/BOOT-INF/classes /powerjob/classes
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
# 设置其他环境变量
ENV APP_NAME=powerjob-worker-agent
# 暴露端口（AKKA-Client）
EXPOSE 27777
ENTRYPOINT ["/entrypoint.sh"]

