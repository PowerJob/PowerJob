FROM mysql/mysql-server:8.0.30

MAINTAINER dudiao(idudaio@163.com)

ENV TZ=Asia/Shanghai

RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY sql/01schema.sql /docker-entrypoint-initdb.d

COPY sql/02worker-samples.sql /docker-entrypoint-initdb.d
