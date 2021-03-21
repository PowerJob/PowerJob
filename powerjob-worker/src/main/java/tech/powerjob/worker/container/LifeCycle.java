package tech.powerjob.worker.container;

/**
 * 生命周期
 *
 * @author tjq
 * @since 2020/5/15
 */
public interface LifeCycle {

    /**
     * 初始化
     * @throws Exception 初始化异常
     */
    void init() throws Exception;

    /**
     * 销毁
     * @throws Exception 销毁异常
     */
    void destroy() throws Exception;
}
