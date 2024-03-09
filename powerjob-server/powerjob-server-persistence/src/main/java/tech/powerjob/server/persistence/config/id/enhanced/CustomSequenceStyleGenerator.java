package tech.powerjob.server.persistence.config.id.enhanced;

import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.service.ServiceRegistry;

import java.util.Properties;

/**
 * description 重写利用序列生成id策略生成器
 *
 * @author jian chen jiang
 * date 2024/3/1 22:18
 */
public class CustomSequenceStyleGenerator extends SequenceStyleGenerator {
    @Override
    protected QualifiedName determineSequenceName(Properties params, Dialect dialect, JdbcEnvironment jdbcEnv, ServiceRegistry serviceRegistry) {
        if(!params.contains(CONFIG_PREFER_SEQUENCE_PER_ENTITY)){
            //让每个表用自己的序列生成器 ，hibernate 6系列 这个是默认配置故可以不用配置了
            params.setProperty(CONFIG_PREFER_SEQUENCE_PER_ENTITY,"true");
        }
        return super.determineSequenceName(params, dialect, jdbcEnv, serviceRegistry);
    }
}
