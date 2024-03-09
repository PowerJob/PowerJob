package tech.powerjob.server.web.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * AppBaseVO
 *
 * @author tjq
 * @since 2024/2/13
 */
@Getter
@Setter
public class AppBaseVO implements Serializable {

    protected Long id;

    protected String appName;

    protected Long namespaceId;
    /**
     * 描述
     */
    protected String title;
}
