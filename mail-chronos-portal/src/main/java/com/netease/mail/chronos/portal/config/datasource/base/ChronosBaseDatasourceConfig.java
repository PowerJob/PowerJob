package com.netease.mail.chronos.portal.config.datasource.base;

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
@MapperScan(basePackages = ChronosBaseDatasourceConfig.PACKAGE, sqlSessionFactoryRef = "chronosBaseSqlSessionFactory")
@Getter
public class ChronosBaseDatasourceConfig extends MyBatisDataSourceConfigSupport {

    @Value("${chronos.table.prefix:}")
    private String tablePrefix;

    public static final String PACKAGE = "com.netease.mail.chronos.portal.mapper.base";

    public static final String XML_LOCATION = "classpath*:mapper/base/*.xml";

    public static final String MYBATIS_CONFIG_FILE = "META-INF/config/mybatis/setting.xml";

    @Bean(name = "chronosBaseDatasource")
    @ConfigurationProperties(prefix = "datasource.chronos.base")
    public HikariDataSource dataSource() {
        return new HikariDataSource();
    }

    @Bean(name = "chronosBaseSqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("chronosBaseDatasource") DataSource dataSource) throws Exception {
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
