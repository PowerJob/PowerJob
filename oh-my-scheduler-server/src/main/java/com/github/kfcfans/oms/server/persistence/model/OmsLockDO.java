package com.github.kfcfans.oms.server.persistence.model;

import lombok.Data;
import lombok.NoArgsConstructor;

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
@Table(name = "oms_lock", uniqueConstraints = {@UniqueConstraint(name = "lockNameUK", columnNames = {"lockName"})})
public class OmsLockDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lockName;
    private String ownerIP;

    private Date gmtCreate;
    private Date gmtModified;

    public OmsLockDO(String lockName, String ownerIP) {
        this.lockName = lockName;
        this.ownerIP = ownerIP;
        this.gmtCreate = new Date();
        this.gmtModified = this.gmtCreate;
    }
}
