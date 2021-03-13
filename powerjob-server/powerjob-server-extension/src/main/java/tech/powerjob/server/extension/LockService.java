package tech.powerjob.server.extension;

/**
 * 锁服务，所有方法都不允许抛出任何异常！
 *
 * @author tjq
 * @since 2020/4/2
 */
public interface LockService {

    /**
     * 上锁（获取锁），立即返回，不会阻塞等待锁
     * @param name 锁名称
     * @param maxLockTime 最长持有锁的时间，单位毫秒（ms）
     * @return true -> 获取到锁，false -> 未获取到锁
     */
    boolean tryLock(String name, long maxLockTime);

    /**
     * 释放锁
     * @param name 锁名称
     */
    void unlock(String name);
}
