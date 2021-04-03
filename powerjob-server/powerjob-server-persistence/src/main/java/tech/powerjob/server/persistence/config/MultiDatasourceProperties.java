package tech.powerjob.server.persistence.config;

import com.google.common.collect.Maps;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 多重数据源配置
 *
 * @author Kung Yao
 * @since 2020/4/27
 */
@Component
@ConfigurationProperties("spring.datasource")
public class MultiDatasourceProperties {

    private DataSourceProperties remote = new DataSourceProperties();

    private DataSourceProperties local = new DataSourceProperties();


    public static class DataSourceProperties {

        private HibernateProperties hibernate = new HibernateProperties();

        public void setHibernate(HibernateProperties hibernate) {
            this.hibernate = hibernate;
        }

        public HibernateProperties getHibernate() {
            return hibernate;
        }
    }


    public static class HibernateProperties {

        private Map<String, String> properties = Maps.newHashMap();

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }

    public void setLocal(DataSourceProperties local) {
        this.local = local;
    }

    public void setRemote(DataSourceProperties remote) {
        this.remote = remote;
    }

    public DataSourceProperties getLocal() {
        return local;
    }

    public DataSourceProperties getRemote() {
        return remote;
    }
}
