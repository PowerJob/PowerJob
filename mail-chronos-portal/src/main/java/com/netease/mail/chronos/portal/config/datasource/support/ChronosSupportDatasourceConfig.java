package com.netease.mail.chronos.portal.config.datasource.support;

import com.netease.mail.chronos.portal.config.datasource.MyBatisDataSourceConfigSupport;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * @author Echo009
 * @since 2021/8/26
 */
@MapperScan(basePackages = ChronosSupportDatasourceConfig.PACKAGE, sqlSessionFactoryRef = "chronosSupportSqlSessionFactory")
@Getter
public class ChronosSupportDatasourceConfig extends MyBatisDataSourceConfigSupport {

    @Value("${chronos.table.prefix:}")
    private String tablePrefix;

    public static final String PACKAGE = "com.netease.mail.chronos.portal.mapper.support";

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
    @Override
    public String getMyBatisConfigFilePath() {
        return MYBATIS_CONFIG_FILE;
    }

    /**
     * 获取 mapper 文件路径
     *
     * @return mapper 文件路径
     */
    @Override
    public String getMyBatisXmlMapperLocation() {
        return XML_LOCATION;
    }
}