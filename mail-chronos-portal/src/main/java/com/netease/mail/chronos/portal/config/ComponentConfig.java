package com.netease.mail.chronos.portal.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Echo009
 * @since 2021/9/21
 */
@Configuration
public class ComponentConfig {


    @Bean("remindTaskIdGenerator")
    public Snowflake getRemindTaskIdGenerator(){
        return IdUtil.getSnowflake();
    }




}
