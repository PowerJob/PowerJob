package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 应用信息表
 *
 * @author tjq
 * @since 2020/3/30
 */
@Data
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(name = "uidx01_app_info", columnNames = {"appName"})})
public class AppInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '应用ID'")
    private Long id;


    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '应用名称'")
    private String appName;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '描述'")
    private String title;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '应用分组密码'")
    private String password;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '当前负责该 appName 旗下任务调度的server地址，IP:Port（注意，该地址为ActorSystem地址，而不是HTTP地址，两者端口不同）'")
    private String currentServer;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '命名空间ID，外键关联'")
    private Long namespaceId;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '管理标签'")
    private String tags;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '扩展字段'")
    private String extra;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '创建者ID'")
    private Long creator;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '修改者ID'")
    private Long modifier;
}