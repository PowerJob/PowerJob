package com.netease.mail.chronos.executor.config.datasource;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author Echo009
 * @since 2021/8/26
 */
@MapperScan(basePackages = ChronosSupportDatasourceConfig.PACKAGE, sqlSessionFactoryRef = "chronosSupportSqlSessionFactory")
@Getter
@Configuration
public class ChronosSupportDatasourceConfig  {

    @Value("${chronos.table.prefix:}")
    private String tablePrefix;

    public static final String PACKAGE = "com.netease.mail.chronos.executor.support.mapper";

    public static final String XML_LOCATION = "classpath*:mapper/support/*.xml";

    public static final String MYBATIS_CONFIG_FILE = "META-INF/config/mybatis/setting.xml";

    @Bean(name = "chronosSupportDatasource")
    @ConfigurationProperties(prefix = "datasource.chronos.support")
    public HikariDataSource dataSource() {
        return new HikariDataSource();
    }

    @Bean(name = "chronosSupportSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("chronosSupportDatasource") DataSource dataSource) throws Exception {
        return constructSqlSessionFactory(dataSource);
    }


    /**
     * 获取配置文件路径
     *
     * @return 配置文件路径
     */
    public String getMyBatisConfigFilePath() {
        return MYBATIS_CONFIG_FILE;
    }

    /**
     * 获取 mapper 文件路径
     *
     * @return mapper 文件路径
     */
    public String getMyBatisXmlMapperLocation() {
        return XML_LOCATION;
    }




    public SqlSessionFactory constructSqlSessionFactory(DataSource dataSource) throws Exception {
        final MybatisSqlSessionFactoryBean sessionFactoryBean = new MybatisSqlSessionFactoryBean();

        sessionFactoryBean.setDataSource(dataSource);
        // 配置文件
        InputStream inputStream = Resources.getResourceAsStream(getMyBatisConfigFilePath());
        Resource inputStreamSource = new InputStreamResource(inputStream);
        sessionFactoryBean.setConfigLocation(inputStreamSource);
        // mapper XML 文件路径
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactoryBean.setMapperLocations(resolver.getResources(getMyBatisXmlMapperLocation()));

        // support dynamic table name prefix
        if (StringUtils.isNotBlank(getTablePrefix())){
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor = new DynamicTableNameInnerInterceptor();
            dynamicTableNameInnerInterceptor.setTableNameHandler(((sql, tableName) -> getTablePrefix() +tableName));
            interceptor.addInnerInterceptor(dynamicTableNameInnerInterceptor);
            sessionFactoryBean.setPlugins(interceptor);
        }

        Objects.requireNonNull(sessionFactoryBean.getObject()).getConfiguration().setMapUnderscoreToCamelCase(true);
        return sessionFactoryBean.getObject();
    }


}
