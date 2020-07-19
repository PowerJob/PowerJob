package com.github.kfcfans.powerjob.server.persistence.config;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
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

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
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
        basePackages = CoreJpaConfig.CORE_PACKAGES,
        // 实体管理bean名称
        entityManagerFactoryRef = "coreEntityManagerFactory",
        // 事务管理bean名称
        transactionManagerRef = "coreTransactionManager"
)
public class CoreJpaConfig {

    @Resource(name = "omsCoreDatasource")
    private DataSource omsCoreDatasource;

    @Resource
    private PowerJobPhysicalNamingStrategy powerJobPhysicalNamingStrategy;

    public static final String CORE_PACKAGES = "com.github.kfcfans.powerjob.server.persistence.core";

    /**
     * 生成配置文件，包括 JPA配置文件和Hibernate配置文件，相当于一下三个配置
     * spring.jpa.show-sql=false
     * spring.jpa.open-in-view=false
     * spring.jpa.hibernate.ddl-auto=update
     *
     * @return 配置Map
     */
    private Map<String, Object> genDatasourceProperties() {

        JpaProperties jpaProperties = new JpaProperties();
        jpaProperties.setOpenInView(false);
        jpaProperties.setShowSql(false);

        HibernateProperties hibernateProperties = new HibernateProperties();
        hibernateProperties.setDdlAuto("update");

        // 配置JPA自定义表名称策略
        HibernateSettings hibernateSettings = new HibernateSettings();
        List<HibernatePropertiesCustomizer> customizers = new ArrayList<>();
        customizers.add(
                new NamingStrategiesHibernatePropertiesCustomizer(powerJobPhysicalNamingStrategy, null));
        hibernateSettings.hibernatePropertiesCustomizers(customizers);
        return hibernateProperties.determineHibernateProperties(jpaProperties.getProperties(), hibernateSettings);
    }

    @Primary
    @Bean(name = "coreEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean initCoreEntityManagerFactory(EntityManagerFactoryBuilder builder) {

        return builder
                .dataSource(omsCoreDatasource)
                .properties(genDatasourceProperties())
                .packages(CORE_PACKAGES)
                .persistenceUnit("corePersistenceUnit")
                .build();
    }


    @Primary
    @Bean(name = "coreTransactionManager")
    public PlatformTransactionManager initCoreTransactionManager(EntityManagerFactoryBuilder builder) {
        return new JpaTransactionManager(Objects.requireNonNull(initCoreEntityManagerFactory(builder).getObject()));
    }


    /**
     * 参考 HibernateJpaConfiguration.NamingStrategiesHibernatePropertiesCustomizer
     */
    private static class NamingStrategiesHibernatePropertiesCustomizer implements HibernatePropertiesCustomizer {

        private final PhysicalNamingStrategy physicalNamingStrategy;

        private final ImplicitNamingStrategy implicitNamingStrategy;

        NamingStrategiesHibernatePropertiesCustomizer(PhysicalNamingStrategy physicalNamingStrategy,
                                                      ImplicitNamingStrategy implicitNamingStrategy) {
            this.physicalNamingStrategy = physicalNamingStrategy;
            this.implicitNamingStrategy = implicitNamingStrategy;
        }

        @Override
        public void customize(Map<String, Object> hibernateProperties) {
            if (this.physicalNamingStrategy != null) {
                hibernateProperties.put("hibernate.physical_naming_strategy", this.physicalNamingStrategy);
            }
            if (this.implicitNamingStrategy != null) {
                hibernateProperties.put("hibernate.implicit_naming_strategy", this.implicitNamingStrategy);
            }
        }

    }
}
