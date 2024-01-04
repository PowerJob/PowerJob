package tech.powerjob.server.common.spring.condition;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import tech.powerjob.common.utils.CollectionUtils;

import java.util.List;

/**
 * PropertyAndOneBeanCondition
 * 存在多个接口实现时的唯一规则
 *
 * @author tjq
 * @since 2023/7/30
 */
@Slf4j
public abstract class PropertyAndOneBeanCondition implements Condition {

    /**
     * 配置中存在任意一个 Key 即可加载该 Bean，空代表不校验
     * @return Keys
     */
    protected abstract List<String> anyConfigKey();

    /**
     * Bean 唯一性校验，空代表不校验
     * @return beanType
     */
    protected abstract Class<?> beanType();

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

        boolean anyCfgExist = checkAnyConfigExist(context);
        log.info("[PropertyAndOneBeanCondition] [{}] check any config exist result with keys={}: {}", thisName(), anyConfigKey(), anyCfgExist);
        if (!anyCfgExist) {
            return false;
        }

        Class<?> beanType = beanType();
        if (beanType == null) {
            return true;
        }
        boolean exist = checkBeanExist(context);
        log.info("[PropertyAndOneBeanCondition] [{}] bean of type[{}] exist check result: {}", thisName(), beanType.getSimpleName(), exist);
        if (exist) {
            log.info("[PropertyAndOneBeanCondition] [{}] bean of type[{}] already exist, skip load!", thisName(), beanType.getSimpleName());
            return false;
        }
        return true;
    }

    private boolean checkAnyConfigExist(ConditionContext context) {
        Environment environment = context.getEnvironment();

        List<String> keys = anyConfigKey();

        if (CollectionUtils.isEmpty(keys)) {
            return true;
        }

        // 判断前缀是否符合，任意满足即可
        for (String key : keys) {
            if (StringUtils.isNotEmpty(environment.getProperty(key))) {
                return true;
            }
        }

        return false;
    }

    private boolean checkBeanExist(ConditionContext context) {

        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory == null) {
            return false;
        }
        try {
            beanFactory.getBean(beanType());
            return true;
        } catch (NoSuchBeanDefinitionException ignore) {
            return false;
        }
    }

    private String thisName() {
        return this.getClass().getSimpleName();
    }
}
