FROM harbor-qzbeta.mail.netease.com/library/java/spring-napm-base:1.0.14

RUN mkdir /home/temp \
 && cd /home/temp \
 && curl https://bootstrap.pypa.io/pip/2.7/get-pip.py -o get-pip.py \
 && python get-pip.py \
 && pip install requests \
 && rm -rf /home/temp

ENV APP_PORT=18087 \
  DEPLOY_ENV=test \
  LOG_DIR=/home/logs \
  DEVOPS_DIR=/home/devops

COPY ./*/*/target/*.zip ./*/target/*.zip ./target/*.zip ${APP_DIR}/

WORKDIR ${APP_DIR}

RUN mkdir -p ${LOG_DIR} ${DEVOPS_DIR} \
  && unzip -d .tmp *.zip; rm *.zip \
  && find .tmp -mindepth 2 -maxdepth 2 ! -path ".tmp/assets*" -print0 | xargs -0 -r mv -t .tmp/ \
  && SRV_NAME=`head -n 1 .tmp/service-${DEPLOY_ENV}.conf | awk -F '"' '{print $2}'` \
  && PROD_NAME=`echo ${SRV_NAME} | awk '{split($0,a,"-"); print a[1]}'` \
  && mkdir ${SRV_NAME} \
  && mv .tmp/*.jar ${SRV_NAME}/ \
  && ([ ! -d .tmp/assets ] || mv .tmp/assets ${SRV_NAME}/assets) \
  && mv .tmp/jmxremote-${DEPLOY_ENV}.access ${SRV_NAME}/jmxremote.access \
  && mv .tmp/jmxremote-${DEPLOY_ENV}.password ${SRV_NAME}/jmxremote.password \
  && mv .tmp/service-${DEPLOY_ENV}.conf ${SRV_NAME}/service.conf \
  && sed "s/P_NAME/3-${PROD_NAME}/g" lib/napm-agent/conf/napm-agent.properties.tmpl | sed "s/S_NAME/${SRV_NAME}/g" > ${SRV_NAME}/napm-agent.properties \
  && rm -r .tmp; echo "${APP_DIR}/${SRV_NAME}" >> .projects

EXPOSE ${APP_PORT}/tcp

ENTRYPOINT PROJ_DIR=`head -n 1 .projects`; sh service.sh -d "${PROJ_DIR}" -j "-javaagent:${APP_DIR}/lib/napm-agent/napm-java-rewriter.jar=conf=${PROJ_DIR}/napm-agent.properties" start