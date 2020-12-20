package com.github.kfcfans.powerjob.server.service.id;

import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.github.kfcfans.powerjob.server.extension.ServerIdProvider;
import com.github.kfcfans.powerjob.server.persistence.core.model.ServerInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.ServerInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 默认服务器 ID 生成策略，不适用于 Server 频繁重启且变化 IP 的场景
 * @author user
 */
@Slf4j
@Service
public class DefaultServerIdProvider implements ServerIdProvider {

    private final Long id;

    public DefaultServerIdProvider(ServerInfoRepository serverInfoRepository) {
        String ip = NetUtils.getLocalHost();
        ServerInfoDO server = serverInfoRepository.findByIp(ip);

        if (server == null) {
            ServerInfoDO newServerInfo = new ServerInfoDO(ip);
            server = serverInfoRepository.saveAndFlush(newServerInfo);
        }
        this.id = server.getId();

        log.info("[DefaultServerIdProvider] address:{},id:{}", ip, id);
    }

    @Override
    public long getServerId() {
        return id;
    }
}
