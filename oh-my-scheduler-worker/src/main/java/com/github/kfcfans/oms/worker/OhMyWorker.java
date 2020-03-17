package com.github.kfcfans.oms.worker;

import akka.actor.ActorSystem;
import com.github.kfcfans.oms.worker.common.OhMyConfig;
import com.github.kfcfans.oms.worker.common.utils.SpringUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 客户端启动类
 *
 * @author KFCFans
 * @since 2020/3/16
 */
public class OhMyWorker implements ApplicationContextAware, InitializingBean {

    public static ActorSystem actorSystem;
    @Getter
    @Setter
    private static OhMyConfig config;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.inject(applicationContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    public void init() {

    }
}
