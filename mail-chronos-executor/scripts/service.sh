#!/usr/bin/env bash
### BEGIN INIT INFO
# Provides:          <NAME>
# Required-Start:    $local_fs $network $named $time $syslog
# Required-Stop:     $local_fs $network $named $time $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Description:       <DESCRIPTION>
# Version:           2.0.6
### END INIT INFO


__dir_path="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
__dir_name="$(basename ${__dir_path})"

cd ${__dir_path}

if [[ ! -f service.conf ]]; then
    echo "config file not found"
    exit 1
else
    source ./service.conf
fi

TEMP=`getopt -o j: -- "$@"`

eval set -- "${TEMP}"

while true ; do
    case "$1" in
        -j) JVM_OPTS="${JVM_OPTS} $2" ; shift 2 ;;
        --) shift ; break ;;
        *) shift ;;
    esac
done

#LOCAL_IP=`/sbin/ip addr | fgrep inet | awk '$2~/^192.168.|^172.16.|^10./{print $2}' | cut -d\/ -f1 | head -n1`
LOCAL_IP="0.0.0.0"

if [  ! "${ACTIVE_PROFILES}" = "" ]; then
  JVM_OPTS="${JVM_OPTS} -Dspring.profiles.active=${ACTIVE_PROFILES}"
fi

if [ ! "${SERVER_PORT}" = "" ]; then
  JVM_OPTS="${JVM_OPTS} -Dserver.port=${SERVER_PORT}"
  JVM_OPTS="${JVM_OPTS} -Dserver.address=${LOCAL_IP}"
fi

if [ ! "${SERVER_CONTEXT_PATH}" = "" ]; then
  JVM_OPTS="${JVM_OPTS} -Dserver.servlet.context-path=${SERVER_CONTEXT_PATH}"
fi

if [ ! "${MANAGER_PORT}" = "" ]; then
  JVM_OPTS="${JVM_OPTS} -Dmanagement.server.port=${MANAGER_PORT}"
fi

JVM_OPTS="${JVM_OPTS} -Dmanagement.server.address=${LOCAL_IP}"


if [ "${ENABLE_APR}" = 1 ]; then
  JVM_OPTS="${JVM_OPTS} -Djava.library.path=${NATIVE_PATH} -Dserver.tomcat.apr.enabled=true"
fi

if [ "${ENABLE_PINPOINT}" = 1 ]; then

  if [ "${PINPOINT_AGENT}" = "" ]; then
    PINPOINT_AGENT="/home/devops/pp/pp-agent/pinpoint-bootstrap.jar"
  fi

  if [ "${PINPOINT_APPLICATION_NAME}" = "" ]; then
    PINPOINT_APPLICATION_NAME=${NAME}
  fi

  if [ "${PINPOINT_AGENT_ID_PREFIX}" = "" ]; then
    PINPOINT_AGENT_ID_PREFIX=${PINPOINT_APPLICATION_NAME}
  fi

  PINPOINT_AGENT_ID=`echo ${LOCAL_IP} | cut -d "." -f 3- | awk -v name=${PINPOINT_AGENT_ID_PREFIX} '{print name "-" $1}'`

  JVM_OPTS="$JVM_OPTS -javaagent:$PINPOINT_AGENT"
  JVM_OPTS="$JVM_OPTS -Dpinpoint.applicationName=$PINPOINT_APPLICATION_NAME"
  JVM_OPTS="$JVM_OPTS -Dpinpoint.agentId=$PINPOINT_AGENT_ID"
fi

if [ "${ENABLE_DRUID_MONITOR}" = 1 ]; then
  JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote"
  JVM_OPTS="$JVM_OPTS -Djava.rmi.server.hostname=${LOCAL_IP}"
  JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.port=${DRUID_JMX_PORT}"
  JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.authenticate=true"
  JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.password.file=${DRUID_JMX_PASSWORD_FILE}"
  JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.access.file=${DRUID_JMX_ACCESS_FILE}"
  JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.ssl=false"
fi

if [ "${ENABLE_CONFIG_CENTER}" = 1 ]; then
  JVM_OPTS="$JVM_OPTS -Denv=${CONFIG_CENTER_ENV} -Dapp.id=${CONFIG_CENTER_APP_ID}"
fi

APP_PATH="./${NAME}-${VERSION}.jar"
SCRIPT="${JAVA_CMD} ${JVM_OPTS} -jar ${APP_PATH}"

if [ ! -f ${APP_PATH} ]; then
  echo "${APP_PATH} no found"
  exit 1
fi

PID_FILE="./${NAME}.pid"
LOG_DIR="${LOG_PATH}/${NAME}"
LOG_FILE="${LOG_DIR}/${NAME}.log"

start() {
  if [ -f "${PID_FILE}" ] && [ -s "${PID_FILE}" ] && kill -0 $(cat "${PID_FILE}"); then
    echo 'Service already running' >&2
    return 1
  fi
  echo 'Starting service…' >&2
  if [ ! -d "${LOG_DIR}" ]; then
    mkdir -p ${LOG_DIR}
    echo "Create dir ${LOG_DIR}"
  fi
  #echo ${SCRIPT}
  ${SCRIPT} >> "${LOG_FILE}" 2>&1
  PID=$!

  declare -i counter=0
  declare -i max_counter=240 # 240*1=240s
  declare -i total_time=0

  if [ "${HEALTH_CHECK_PATH}" = "" ]; then
    echo "WARN! HEALTH_CHECK_PATH no set"
  else
    HEALTH_CHECK_URL="http://${LOCAL_IP}:${SERVER_PORT}${SERVER_CONTEXT_PATH}${HEALTH_CHECK_PATH}"
  fi

  printf "Waiting for server startup"
  until [[ (( counter -ge max_counter )) || "${HEALTH_CHECK_URL}" = "" ]];
  do
    printf "."
    counter+=1
    sleep 1

    if [[ $(curl -s -o /dev/null -w "%{http_code}"  ${HEALTH_CHECK_URL} | grep 200) != "" ]]; then
      break
    fi
  done

  total_time=counter*1

  if [[ (( counter -ge max_counter )) ]];
  then
    printf "\n$(date) Server failed to start in $total_time seconds!\n"
    return 1;
  fi

  echo ${PID} > "${PID_FILE}"
  printf "\n$(date) Server started in $total_time seconds! The PID is ${PID}\n"
  return 0;
}

stop() {
  if [ ! -f "${PID_FILE}" ] || ! kill -0 $(cat "${PID_FILE}"); then
    echo 'Service not running' >&2
    return 0
  fi

  PID=$(cat "${PID_FILE}")
  kill -15 ${PID}

  declare -i counter=0
  declare -i max_counter=240 # 240*1=240s
  declare -i total_time=0

  printf "Stopping service"
  until [[ counter -ge max_counter ]] ||  ! kill -0 ${PID} 2>/dev/null ;
  do
    printf "."
    counter+=1
    sleep 1
  done

  total_time=counter*1

  if [[ (( counter -ge max_counter )) ]];
  then
    printf "\n$(date) Server failed to stop in $total_time seconds!\n"
    return 1;
  fi

  rm -f "${PID_FILE}"
  printf "\n$(date) Server stopped in $total_time seconds!\n"
}


status() {
    if [ "$2" = "health_check" ]; then
        printf "%-50s" "Checking Service health ..."
        HEALTH_CHECK=1
    else
        printf "%-50s" "Checking Service..."
    fi

    if [ -f "${PID_FILE}" ] && [ -s "${PID_FILE}" ]; then
        PID=$(cat "${PID_FILE}")
            if [ -z "$(ps aux | grep ${PID} | grep -v grep)" ]; then
                printf "%s\n" "The process appears to be dead but pidfile still exists"
                rm -f "${PID_FILE}"
                if [ -n "${HEALTH_CHECK}" ]; then
                    return 1
                fi
            else
                echo "Running, the PID is ${PID}"
            fi
    else
        printf "%s\n" "Service not running"
        if [ -n "${HEALTH_CHECK}" ]; then
            return 1
        fi
    fi
}

down() {
    if [ ! "${MANAGER_PORT}" = "" ]; then
        SERVICE_REMOVE_URL="http://${LOCAL_IP}:${MANAGER_PORT}/admin/service-registry?status=DOWN"
        if [[ $(curl -s -o /dev/null -H 'Content-Type: application/vnd.spring-boot.actuator.v2+json;charset=UTF-8' -w "%{http_code}" -X "POST" ${SERVICE_REMOVE_URL} | grep 200) != "" ]]; then
            printf "%s\n" "Service is down"
        else
            printf "%s\n" "Service down failure"
        fi
    else
        printf "%s\n" "Manager port not found"
    fi
}

up() {
    if [ ! "${MANAGER_PORT}" = "" ]; then
        SERVICE_REMOVE_URL="http://${LOCAL_IP}:${MANAGER_PORT}/admin/service-registry?status=UP"
        if [[ $(curl -s -o /dev/null -H 'Content-Type: application/vnd.spring-boot.actuator.v2+json;charset=UTF-8' -w "%{http_code}" -X "POST" ${SERVICE_REMOVE_URL} | grep 200) != "" ]]; then
            printf "%s\n" "Service is up"
        else
            printf "%s\n" "Service up failure"
        fi
    else
        printf "%s\n" "Manager port not found"
    fi
}

logs() {
    if [ -f "${LOG_FILE}" ]; then
        printf "%-50s\n" "tail -fn100 ${LOG_FILE}"
        echo "--------------"
        tail -fn100 "${LOG_FILE}"
    else
        echo "Logfile:${LOG_FILE} not exits"
    fi
}


case $1 in
  start)
    start
    ;;
  stop)
    stop
    ;;
  status)
    status $@
    ;;
  restart)
    stop
    start
    ;;
  down)
    down
    ;;
  up)
    up
    ;;
  logs)
    logs
    ;;
  *)
    echo "Usage: $0 {start|stop|down|up|status|restart|logs}"
esac
