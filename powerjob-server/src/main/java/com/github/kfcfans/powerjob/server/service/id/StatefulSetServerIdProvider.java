package com.github.kfcfans.powerjob.server.service.id;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author user
 */
public class StatefulSetServerIdProvider implements ServerIdProvider {
  /**
   * xxx-1,aa-bb-2
   */
  private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^.*-([0-9]+)$");
  
  @Override
  public long serverId() {
    try {
      String hostname = InetAddress.getLocalHost().getHostName();
      Matcher matcher = HOSTNAME_PATTERN.matcher(hostname);
      if (matcher.matches()) {
        return Long.parseLong(matcher.group(1));
      }
      throw new RuntimeException(String.format("hostname=%s not match %s", hostname, HOSTNAME_PATTERN.toString()));
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }
}
