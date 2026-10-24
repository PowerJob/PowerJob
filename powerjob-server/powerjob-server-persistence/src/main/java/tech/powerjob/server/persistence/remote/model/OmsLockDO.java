package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 数据库锁
 *
 * @author tjq
 * @since 2020/4/2
 */
@Data
@Entity
@NoArgsConstructor
@Table(uniqueConstraints = {@UniqueConstraint(name = "uidx01_oms_lock", columnNames = {"lockName"})})
public class OmsLockDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '序号ID'")
    private Long id;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '名称'")
    private String lockName;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '拥有者IP'")
    private String ownerIP;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '最长持锁时间'")
    private Long maxLockTime;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;

    public OmsLockDO(String lockName, String ownerIP, Long maxLockTime) {
        this.lockName = lockName;
        this.ownerIP = ownerIP;
        this.maxLockTime = maxLockTime;
        this.gmtCreate = new Date();
        this.gmtModified = this.gmtCreate;
    }
}