package com.netease.mail.chronos.portal.config.datasource;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author Echo009
 * @since 2021/9/21
 */
public abstract class MyBatisDataSourceConfigSupport {

    /**
     * 获取配置文件路径
     * @return 配置文件路径
     */

    public abstract String getMyBatisConfigFilePath();

    /**
     * 获取 mapper 文件路径
     * @return mapper 文件路径
     */
    public abstract String getMyBatisXmlMapperLocation();

    /**
     * 获取表前缀
     * @return table prefix
     */
    public abstract String getTablePrefix();




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
        final String tablePrefix = getTablePrefix();
        if (StringUtils.isNotBlank(getTablePrefix())){
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor = new DynamicTableNameInnerInterceptor();
            dynamicTableNameInnerInterceptor.setTableNameHandler(((sql, tableName) -> tablePrefix +tableName));
            interceptor.addInnerInterceptor(dynamicTableNameInnerInterceptor);
            sessionFactoryBean.setPlugins(interceptor);
        }

        Objects.requireNonNull(sessionFactoryBean.getObject()).getConfiguration().setMapUnderscoreToCamelCase(true);
        return sessionFactoryBean.getObject();
    }


}
