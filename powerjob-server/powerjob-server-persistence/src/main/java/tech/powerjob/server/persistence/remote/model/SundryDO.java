package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 杂项
 * KKV 表存一些配置数据
 *
 * @author tjq
 * @since 2024/2/15
 */
@Data
@Entity
@NoArgsConstructor
@Table(uniqueConstraints = {@UniqueConstraint(name = "uidx01_sundry", columnNames = {"pkey", "skey"})})
public class SundryDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '杂项ID'")
    private Long id;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT 'PKEY'")
    private String pkey;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT 'SKEY'")
    private String skey;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '内容'")
    private String content;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '其他参数'")
    private String extra;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;
}