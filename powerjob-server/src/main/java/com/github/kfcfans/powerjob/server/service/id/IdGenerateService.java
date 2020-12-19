package com.github.kfcfans.powerjob.server.service.id;

import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.ServerInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.ServerInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
    public IdGenerateService(@Qualifier("serverIdProvider") ServerIdProvider serverIdProvider) {
        long id = serverIdProvider.serverId();
        snowFlakeIdGenerator = new SnowFlakeIdGenerator(DATA_CENTER_ID, id);
        String ip = NetUtils.getLocalHost();

        log.info("[IdGenerateService] init snowflake for server(address={}) by machineId({}).", ip, id);
    }

    /**
     * 分配分布式唯一ID
     * @return 分布式唯一ID
     */
    public long allocate() {
        return snowFlakeIdGenerator.nextId();
    }

}
