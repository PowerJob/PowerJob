package tech.powerjob.server.initializer;

/**
 * 系统初始化服务
 *
 * @author tjq
 * @since 2024/2/15
 */
public interface SystemInitializeService {

    String GOAL_INIT_ADMIN = "goal_init_admin";
    String GOAL_INIT_NAMESPACE = "goal_init_namespace";


    /**
     * 初始化超级管理员
     */
    void initAdmin();

    /**
     * 初始化 namespace
     */
    void initNamespace();
}
