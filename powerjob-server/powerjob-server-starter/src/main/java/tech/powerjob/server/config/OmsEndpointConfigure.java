package tech.powerjob.server.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.websocket.server.ServerEndpointConfig;

/**
 * WebSocket 配置
 * 解决 SpringBoot WebSocket 无法注入对象（@Resource/@Autowired）的问题
 *
 * @author tjq
 * @since 2020/5/17
 */
@Component
public class OmsEndpointConfigure extends ServerEndpointConfig.Configurator implements ApplicationContextAware {

    private static volatile ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        return context.getBean(clazz);
    }
}
