package com.github.kfcfans.powerjob.server.extension;

/**
 * provide unique server ip in the cluster for IdGenerateService
 * @author user
 */
public interface ServerIdProvider {

  /**
   * get number for IdGenerateService
   * @return serverId, must in range [0, 16384)
   */
  long getServerId();
}
