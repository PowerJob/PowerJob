package tech.powerjob.server.persistence.config.id.enhanced;

import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * description 新增或者覆盖之前的的id生成策略
 * 使用方式 在 配置文件配置
 * spring.datasource.remote.hibernate.properties.hibernate.identifier_generator_strategy_provider=tech.powerjob.server.persistence.config.id.enhanced.CustomIdentifierGeneratorStrategyProviderProvider
 * @author jian chen jiang
 * date 2024/3/1 22:08
 */
public class CustomIdentifierGeneratorStrategyProviderProvider implements IdentifierGeneratorStrategyProvider {
    @Override
    public Map<String, Class<?>> getStrategies() {
        Map<String,Class<?>> strages = new HashMap<>();
        //覆盖掉默认的
        strages.put("sequence", CustomSequenceStyleGenerator.class);
        return strages;
    }
}
