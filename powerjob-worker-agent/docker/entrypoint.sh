#!/bin/sh

MAIN_CLASS="tech.powerjob.agent.MainApplication"
TIMESTAMP=`date +%Y%m%d%H%M%S`

MAX_HEAP_SIZE=256m
XMS=${JVM_XMS:-256m}
XMX=${JVM_XMX:-256m}
XMN=${JVM_XMN:-128m}

PARAMS=${PARAMS:---app=haydn --server=powerjob:7700}

JAVA_OPTS=""
JAVA_OPTS="$JAVA_OPTS -server -Xms$XMS -Xmx$XMX -Xmn$XMN"

JAVA_OPTS="$JAVA_OPTS -XX:MaxMetaspaceSize=128m"

JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=${HOME}/logs/jvm-heap-dump-${TIMESTAMP}.hprof"
JAVA_OPT="${JAVA_OPT} -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:G1ReservePercent=25"
JAVA_OPT="${JAVA_OPT} -XX:InitiatingHeapOccupancyPercent=30 -XX:SoftRefLRUPolicyMSPerMB=0"
JAVA_OPT="${JAVA_OPT} -XX:SurvivorRatio=8 -verbose:gc"
JAVA_OPT="${JAVA_OPT} -Xlog:gc*,safepoint:${HOME}/logs/jvm_gc_%p.log:time,uptime:filecount=100,filesize=12M"
JAVA_OPT="${JAVA_OPT} -XX:-OmitStackTraceInFastThrow"
JAVA_OPT="${JAVA_OPT} -XX:+AlwaysPreTouch"
JAVA_OPT="${JAVA_OPT} -XX:MaxDirectMemorySize=${MAX_HEAP_SIZE}"
JAVA_OPT="${JAVA_OPT} -XX:-UseLargePages -XX:-UseBiasedLocking"

exec java ${JAVA_OPTS} \
     -cp "/powerjob/classes:/powerjob/lib/*" \
     -Dspring.jmx.enabled=false \
     ${MAIN_CLASS} $PARAMS
