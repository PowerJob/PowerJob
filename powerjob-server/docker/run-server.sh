docker run -p 7700:7700 -p 10010:10010 -it --rm \
    -e PARAMS="--spring.profiles.active=daily \
    --spring.datasource.core.driver-class-name=org.h2.Driver \
    --spring.datasource.core.jdbc-url=jdbc:h2:file:/tmp/powerjob/h2db;AUTO_SERVER=TRUE \
    --spring.datasource.core.username=powerjob \
    --spring.datasource.core.password=powerjob \
    --oms.storage.dfs.mysql_series.driver=org.h2.Driver \
    --oms.storage.dfs.mysql_series.url=jdbc:h2:file:/tmp/powerjob/h2db;AUTO_SERVER=TRUE \
    --oms.storage.dfs.mysql_series.username=powerjob \
    --oms.storage.dfs.mysql_series.password=powerjob \
    --oms.storage.dfs.mysql_series.auto_create_table=true
    --oms.instanceinfo.retention=30 \
    --oms.container.retention.local=30 \
    --oms.container.retention.remote=-1 \
    --spring.mail.host=smtp-relay.gmail.com \
    --spring.mail.username=test@gmail.com \
    --spring.mail.password=test123 \
    --oms.auth.initiliaze.admin.password=admin" \
    -e JVMOPTIONS=" -Dpowerjob.network.external.address=127.0.0.1 -Xms512m -Xmx1024m -Xmn256m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m" \
    powerjob/powerjob-server:v5.1.2