package com.github.kfcfans.powerjob.server.service.id;

import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.ServerInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.ServerInfoRepository;

/**
 * @author user
 */
public class DefaultServerIdProvider implements ServerIdProvider {
  private final ServerInfoRepository serverInfoRepository;

  private volatile Long id;

  public DefaultServerIdProvider(ServerInfoRepository serverInfoRepository) {
    this.serverInfoRepository = serverInfoRepository;
  }

  @Override
  public long serverId() {
    if (id == null) {
      synchronized (this) {
        if (id == null) {
          String ip = NetUtils.getLocalHost();
          ServerInfoDO server = serverInfoRepository.findByIp(ip);

          if (server == null) {
            ServerInfoDO newServerInfo = new ServerInfoDO(ip);
            server = serverInfoRepository.saveAndFlush(newServerInfo);
          }

          id = server.getId();
        }
      }
    }

    return id;
  }
}
