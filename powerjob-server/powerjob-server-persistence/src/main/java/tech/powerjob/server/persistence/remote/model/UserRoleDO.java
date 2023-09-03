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
     * 角色，${role}_${appId}，比如 Observer_277
     */
    private String role;
    /**
     * 扩展字段
     */
    private String extra;

    private Date gmtCreate;

    private Date gmtModified;
}
