package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * PowerJob 自建登录体系的用户表，只存储使用 PowerJob 自带登录方式登录的用户信息
 *
 * @author tjq
 * @since 2024/2/13
 */
@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uidx01_username", columnNames = {"username"})
})
public class PwjbUserInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    private String username;

    private String password;

    private String extra;

    private Date gmtCreate;

    private Date gmtModified;
}
