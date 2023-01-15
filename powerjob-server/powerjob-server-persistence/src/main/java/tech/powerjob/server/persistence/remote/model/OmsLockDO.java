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
    private Long id;

    private String lockName;

    private String ownerIP;
    /**
     * 最长持有锁的时间
     */
    private Long maxLockTime;

    private Date gmtCreate;

    private Date gmtModified;

    public OmsLockDO(String lockName, String ownerIP, Long maxLockTime) {
        this.lockName = lockName;
        this.ownerIP = ownerIP;
        this.maxLockTime = maxLockTime;
        this.gmtCreate = new Date();
        this.gmtModified = this.gmtCreate;
    }
}
