package com.github.kfcfans.powerjob.server.service.id;

import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.server.extension.ServerIdProvider;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 唯一ID生成服务，使用 Twitter snowflake 算法
 * 机房ID：固定为0，占用2位
 * 机器ID：由 ServerIdProvider 提供
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
    public IdGenerateService(List<ServerIdProvider> serverIdProviders) {

        if (CollectionUtils.isEmpty(serverIdProviders)) {
            throw new PowerJobException("can't find any ServerIdProvider!");
        }

        ServerIdProvider serverIdProvider;
        int severIpProviderNums = serverIdProviders.size();
        if (severIpProviderNums == 1) {
            serverIdProvider = serverIdProviders.get(0);
        } else {
            List<ServerIdProvider> extendServerIpProviders = Lists.newArrayList();
            for (ServerIdProvider sp : serverIdProviders) {
                if (sp instanceof DefaultServerIdProvider) {
                    continue;
                }
                extendServerIpProviders.add(sp);
            }
            int extNum = extendServerIpProviders.size();
            if (extNum != 1) {
                throw new PowerJobException(String.format("find %d ServerIdProvider but just need one, please delete the useless ServerIdProvider!", extNum));
            }
            serverIdProvider = extendServerIpProviders.get(0);
        }

        long id = serverIdProvider.getServerId();
        snowFlakeIdGenerator = new SnowFlakeIdGenerator(DATA_CENTER_ID, id);
        log.info("[IdGenerateService] initialize IdGenerateService successfully, ServerIdProvider:{},ID:{}", serverIdProvider.getClass().getSimpleName(), id);
    }

    /**
     * 分配分布式唯一ID
     * @return 分布式唯一ID
     */
    public long allocate() {
        return snowFlakeIdGenerator.nextId();
    }

}
