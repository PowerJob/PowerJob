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
    private Long id;

    private String appName;
    /**
     * 应用分组密码
     */
    private String password;

    /**
     * 当前负责该 appName 旗下任务调度的server地址，IP:Port（注意，该地址为ActorSystem地址，而不是HTTP地址，两者端口不同）
     * 支持多语言后，尽管引入了 vert.x 的地址，但该字段仍保存 ActorSystem 的地址，vert.x 地址仅在返回给 worker 时特殊处理
     * 原因：框架中很多地方强依赖 currentServer，比如根据该地址来获取需要调度的 app
     */
    private String currentServer;

    private Date gmtCreate;

    private Date gmtModified;
}
