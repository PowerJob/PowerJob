package com.github.kfcfans.oms.server.service.id;

import com.github.kfcfans.common.utils.NetUtils;
import com.github.kfcfans.oms.server.persistence.model.ServerInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.ServerInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 唯一ID生成服务，使用 Twitter snowflake 算法
 * 机房ID：固定为0，占用三位（8个机房怎么样也够了吧）
 * 机器ID：数据库自增，占用7位（最多支持128台机器）
 *
 * @author tjq
 * @since 2020/4/6
 */
@Slf4j
@Service
public class IdGenerateService {

    private SnowFlakeIdGenerator snowFlakeIdGenerator;

    private static final int DATA_CENTER_ID = 0;

    @Autowired
    public IdGenerateService(ServerInfoRepository serverInfoRepository) {

        String ip = NetUtils.getLocalHost();
        ServerInfoDO server = serverInfoRepository.findByIp(ip);

        if (server == null) {
            ServerInfoDO newServerInfo = new ServerInfoDO(ip);
            server = serverInfoRepository.saveAndFlush(newServerInfo);
        }

        Long id = server.getId();
        snowFlakeIdGenerator = new SnowFlakeIdGenerator(DATA_CENTER_ID, id);

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
