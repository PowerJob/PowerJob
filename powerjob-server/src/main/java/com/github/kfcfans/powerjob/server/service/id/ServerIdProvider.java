package com.github.kfcfans.powerjob.server.service.id;

/**
 * @author user
 */
public interface ServerIdProvider {
  /**
   * get number for IdGenerateService
   * @return serverId
   */
  long serverId();
}
