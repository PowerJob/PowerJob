package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 服务器信息表（用于分配服务器唯一ID）
 *
 * @author tjq
 * @since 2020/4/15
 */
@Data
@Entity
@NoArgsConstructor
@Table(
        uniqueConstraints = {@UniqueConstraint(name = "uidx01_server_info", columnNames = "ip")},
        indexes = {@Index(name = "idx01_server_info", columnList = "gmtModified")}
)
public class ServerInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;
    /**
     * 服务器IP地址
     */
    private String ip;

    private Date gmtCreate;

    private Date gmtModified;

    public ServerInfoDO(String ip) {
        this.ip = ip;
        this.gmtCreate = new Date();
        this.gmtModified = this.gmtCreate;
    }
}
