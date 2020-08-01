package com.github.kfcfans.powerjob.server.persistence.core.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 用户信息表
 *
 * @author tjq
 * @since 2020/4/12
 */
@Data
@Entity
@Table
public class UserInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    private String username;
    private String password;

    // 手机号
    private String phone;
    // 邮箱地址
    private String email;

    private Date gmtCreate;
    private Date gmtModified;
}
