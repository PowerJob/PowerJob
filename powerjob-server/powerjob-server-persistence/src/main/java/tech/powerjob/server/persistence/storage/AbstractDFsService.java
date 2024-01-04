package tech.powerjob.server.persistence.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import tech.powerjob.server.extension.dfs.DFsService;

/**
 * AbstractDFsService
 *
 * @author tjq
 * @since 2023/7/28
 */
@Slf4j
public abstract class AbstractDFsService implements DFsService, ApplicationContextAware, DisposableBean {

    protected ApplicationContext applicationContext;

    public AbstractDFsService() {
        log.info("[DFsService] invoke [{}]'s constructor", this.getClass().getName());
    }

    abstract protected void init(ApplicationContext applicationContext);

    protected static final String PROPERTY_KEY = "oms.storage.dfs";

    protected static String fetchProperty(Environment environment, String dfsType, String key) {
        String pKey = String.format("%s.%s.%s", PROPERTY_KEY, dfsType, key);
        return environment.getProperty(pKey);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        log.info("[DFsService] invoke [{}]'s setApplicationContext", this.getClass().getName());
        init(applicationContext);
    }
}
