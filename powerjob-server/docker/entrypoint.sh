#!/bin/sh

MAIN_CLASS="tech.powerjob.server.PowerJobServerApplication"
TIMESTAMP=`date +%Y%m%d%H%M%S`
XMS=${JVM_XMS:-256m}
XMX=${JVM_XMX:-256m}
XMN=${JVM_XMN:-128m}

JAVA_OPTS=""
JAVA_OPTS="$JAVA_OPTS -server -Xms$XMS -Xmx$XMX -Xmn$XMN"
JAVA_OPTS="$JAVA_OPTS -XX:MaxMetaspaceSize=128m"
JAVA_OPTS="$JAVA_OPTS -XX:SurvivorRatio=8"
JAVA_OPTS="$JAVA_OPTS -XX:+UseConcMarkSweepGC"
JAVA_OPTS="$JAVA_OPTS -XX:CMSInitiatingOccupancyFraction=70"
JAVA_OPTS="$JAVA_OPTS -XX:+ScavengeBeforeFullGC"
JAVA_OPTS="$JAVA_OPTS -XX:+CMSScavengeBeforeRemark"
JAVA_OPTS="$JAVA_OPTS -XX:+DisableExplicitGC"
JAVA_OPTS="$JAVA_OPTS -XX:-OmitStackTraceInFastThrow"
JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDateStamps"
JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails"
JAVA_OPTS="$JAVA_OPTS -XX:+UnlockExperimentalVMOptions"
JAVA_OPTS="$JAVA_OPTS -XX:+UseCGroupMemoryLimitForHeap"
JAVA_OPTS="$JAVA_OPTS -verbose:gc"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=${HOME}/logs/jvm-heap-dump-${TIMESTAMP}.hprof"
JAVA_OPTS="$JAVA_OPTS -Xloggc:${HOME}/logs/jvm-gc-${TIMESTAMP}.log"

exec java ${JAVA_OPTS} \
     -cp "/powerjob/classes:/powerjob/lib/*" \
     -Dspring.jmx.enabled=false \
     ${MAIN_CLASS}
