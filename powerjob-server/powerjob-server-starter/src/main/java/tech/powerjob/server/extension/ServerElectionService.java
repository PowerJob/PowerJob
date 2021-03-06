package tech.powerjob.server.extension;

/**
 * 调度服务器选举服务，默认实现为先到先得，可自行接入 Zookeeper 等实现"负载均衡"策略
 *
 * @author tjq
 * @since 2021/2/9
 */
public interface ServerElectionService {

    String elect(Long appId, String protocol, String currentServer);
}
