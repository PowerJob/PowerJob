package tech.powerjob.server.web.request;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 组件上的用户角色信息
 *
 * @author tjq
 * @since 2024/2/12
 */
@Data
@Accessors(chain = true)
public class ComponentUserRoleInfo {
    /**
     * 观察者
     */
    private List<Long> observer;
    /**
     * 测试
     */
    private List<Long> qa;
    /**
     * 开发者
     */
    private List<Long> developer;
    /**
     * 管理员
     */
    private List<Long> admin;

}
