package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 用户角色表
 *
 * @author tjq
 * @since 2023/3/20
 */
@Data
@Entity
@Table(indexes = {
        @Index(name = "uidx01_user_id", columnList = "userId")
})
public class UserRoleDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    /**
     * 授予角色的用户ID
     */
    private Long userId;

    /**
     * 权限范围，namespace 还是 app
     */
    private Integer scope;
    /**
     * 和 scope 一起组成授权目标，比如某个 app 或 某个 namespace
     */
    private Long target;

    /**
     * 角色，比如 Observer
     */
    private Integer role;
    /**
     * 扩展字段
     */
    private String extra;

    private Date gmtCreate;

    private Date gmtModified;
}
