package com.netease.mail.chronos.base;

import com.netease.mail.chronos.executor.config.datasource.DataSourceConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * @author Echo009
 * @since 2021/10/26
 */
@ComponentScan("com.netease.mail.chronos.executor.support.mapper")
@Import(DataSourceConfig.class)
public class DaoBaseContext {


}
