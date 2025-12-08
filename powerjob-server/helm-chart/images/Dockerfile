#ARG powershell_version=7.5.4
#ARG ARCH=${TARGETARCH/arm64/aarch64}
#ARG ARCH=${ARCH/amd64/x86_64}
#FROM powershell AS builder
#ARG powershell_version
#ARG TARGETARCH

FROM powerjob/powerjob-agent:v5.1.2 AS powerjob-build


FROM azul/zulu-openjdk-debian:17 AS powerjob-agent
#ARG powershell_version
ARG TARGETARCH

ENV APP_NAME=powerjob-worker-agent
ENV PARAMS=""
ENV JVMOPTIONS=""

RUN apt-get update && \
    apt-get install -y python-is-python3 curl iproute2 iputils-ping \
    && apt-get clean \
    && apt-get autoclean \
    && rm -rf /var/lib/apt/lists/*

RUN curl -o wait-for-it.sh https://gitee.com/KFCFans/wait-for-it/raw/master/wait-for-it.sh && \
    curl -o powerjob-agent.jar https://github.com/PowerJob/PowerJob/ && \
    curl -Lo dotnet-install.sh https://dot.net/v1/dotnet-install.sh
RUN chmod +x wait-for-it.sh && \
    chmod +x dotnet-install.sh && \
    ./dotnet-install.sh --version latest --runtime dotnet

ENV DOTNET_SYSTEM_GLOBALIZATION_INVARIANT=1
ENV DOTNET_ROOT=/root/.dotnet

#COPY --from=builder /powershell_${powershell_version}-2_${TARGETARCH}.deb powershell_${powershell_version}-2_${TARGETARCH}.deb
COPY --from=powerjob-build /powerjob-agent.jar powerjob-agent.jar

#RUN dpkg -i  powershell_${powershell_version}-2_${TARGETARCH}.deb

EXPOSE 27777

VOLUME /root

ENTRYPOINT ["sh","-c","java $JVMOPTIONS -jar /powerjob-agent.jar $PARAMS"]