package tech.powerjob.server.persistence.storage;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import tech.powerjob.server.extension.dfs.DFsService;

import javax.annotation.Resource;

/**
 * AbstractDFsService
 *
 * @author tjq
 * @since 2023/7/28
 */
public abstract class AbstractDFsService implements DFsService, InitializingBean {

    @Resource
    protected Environment environment;

    protected static final String PROPERTY_KEY = "oms.storage.dfs";

    protected String fetchProperty(String dfsType, String key) {
        String pKey = String.format("%s.%s.%s", PROPERTY_KEY, dfsType, key);
        return environment.getProperty(pKey);
    }

}
