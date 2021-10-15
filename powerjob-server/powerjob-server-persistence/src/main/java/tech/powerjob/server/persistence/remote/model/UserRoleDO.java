package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Echo009
 * @since 2021/10/14
 */
@Data
@Entity
@Table
public class UserRoleDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    private Long userId;

    private String role;

    private Date gmtCreate;

    private Date gmtModified;

}
