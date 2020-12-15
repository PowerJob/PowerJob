package com.github.kfcfans.powerjob.server.service.id;

import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.ServerInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.ServerInfoRepository;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 唯一ID生成服务，使用 Twitter snowflake 算法
 * 机房ID：固定为0，占用2位
 * 机器ID：数据库自增，占用14位（如果频繁部署需要删除数据库重置id）
 *
 * @author tjq
 * @since 2020/4/6
 */
@Slf4j
@Service
public class IdGenerateService {

    private final SnowFlakeIdGenerator snowFlakeIdGenerator;

    private static final int DATA_CENTER_ID = 0;

    @Autowired
    public IdGenerateService(ServerInfoRepository serverInfoRepository,
                             @Qualifier("coreTransactionManager") PlatformTransactionManager platformTransactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        // 服务频繁重启，数据库id自增值过大，使用表锁获取机器id
        snowFlakeIdGenerator = transactionTemplate.execute(action -> {
            List<ServerInfoDO> serverInfos = serverInfoRepository.findAllAndLockTable();
            String ip = NetUtils.getLocalHost();
            ServerInfoDO server = serverInfoRepository.findByIp(ip);
            Long id = null;
            if (server == null) {
                ServerInfoDO newServerInfo = new ServerInfoDO(ip);
                serverInfoRepository.saveAndFlush(newServerInfo);
                id = serverInfos.size() + 1L;
            } else {
                for (int i = 0, len = serverInfos.size(); i < len; i++) {
                    if (Objects.equals(serverInfos.get(i).getId(), server.getId())) {
                        id = i + 1L;
                        break;
                    }
                }
            }
            Assert.notNull(id, "[IdGenerateService] init snowflake error, id is null");
            log.info("[IdGenerateService] init snowflake for server(address={}) by machineId({}).", ip, id);

            return new SnowFlakeIdGenerator(DATA_CENTER_ID, id);
        });
    }

    /**
     * 分配分布式唯一ID
     * @return 分布式唯一ID
     */
    public long allocate() {
        return snowFlakeIdGenerator.nextId();
    }

}
