package tech.powerjob.server.persistence.local;

import tech.powerjob.common.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * 本地的运行时日志
 *
 * @author tjq
 * @since 2020/4/27
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "local_instance_log", indexes = {@Index(columnList = "instanceId")})
public class LocalInstanceLogDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long instanceId;
    /**
     * 日志时间
     */
    private Long logTime;
    /**
     * 日志级别 {@link LogLevel}
     */
    private Integer logLevel;
    /**
     * 日志内容
     */
    @Lob
    @Column(columnDefinition="TEXT")
    private String logContent;

    /**
     * 机器地址
     */
    private String workerAddress;
}
