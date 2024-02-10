package tech.powerjob.server.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权限
 *
 * @author tjq
 * @since 2023/3/20
 */
@Getter
@AllArgsConstructor
public enum Permission {

    /**
     * 不需要权限
     */
    NONE(1),
    /**
     * 读权限，查看控制台数据
     */
    READ(10),
    /**
     * 写权限，新增/修改任务等
     */
    WRITE(20),
    /**
     * 运维权限，比如任务的执行
     */
    OPS(30),
    /**
     * 超级权限
     */
    SU(100)
    ;


    private int v;
}
