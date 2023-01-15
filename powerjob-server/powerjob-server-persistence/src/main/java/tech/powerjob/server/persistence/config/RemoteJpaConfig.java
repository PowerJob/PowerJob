package tech.powerjob.server.persistence.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;

/**
 * 核心数据库 JPA 配置
 *
 * @author tjq
 * @since 2020/4/27
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        // repository包名
        basePackages = RemoteJpaConfig.CORE_PACKAGES,
        // 实体管理bean名称
        entityManagerFactoryRef = "remoteEntityManagerFactory",
        // 事务管理bean名称
        transactionManagerRef = "remoteTransactionManager"
)
public class RemoteJpaConfig {

    public static final String CORE_PACKAGES = "tech.powerjob.server.persistence.remote";

    /**
     * 生成配置文件，包括 JPA配置文件和Hibernate配置文件，相当于以下三个配置
     * spring.jpa.show-sql=false
     * spring.jpa.open-in-view=false
     * spring.jpa.hibernate.ddl-auto=update
     *
     * @return 配置Map
     */
    private static Map<String, Object> genDatasourceProperties() {

        JpaProperties jpaProperties = new JpaProperties();
        jpaProperties.setOpenInView(false);
        jpaProperties.setShowSql(false);

        HibernateProperties hibernateProperties = new HibernateProperties();
        hibernateProperties.setDdlAuto("update");

        // 配置JPA自定义表名称策略
        hibernateProperties.getNaming().setPhysicalStrategy(PowerJobPhysicalNamingStrategy.class.getName());
        HibernateSettings hibernateSettings = new HibernateSettings();
        return hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), hibernateSettings);
    }

    @Primary
    @Bean(name = "remoteEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean initRemoteEntityManagerFactory(@Qualifier("omsRemoteDatasource") DataSource omsRemoteDatasource,@Qualifier("multiDatasourceProperties") MultiDatasourceProperties properties, EntityManagerFactoryBuilder builder) {
        Map<String, Object> datasourceProperties = genDatasourceProperties();
        datasourceProperties.putAll(properties.getRemote().getHibernate().getProperties());
        return builder
                .dataSource(omsRemoteDatasource)
                .properties(datasourceProperties)
                .packages(CORE_PACKAGES)
                .persistenceUnit("remotePersistenceUnit")
                .build();
    }


    @Primary
    @Bean(name = "remoteTransactionManager")
    public PlatformTransactionManager initRemoteTransactionManager(@Qualifier("remoteEntityManagerFactory") LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean) {
        return new JpaTransactionManager(Objects.requireNonNull(localContainerEntityManagerFactoryBean.getObject()));
    }
}
