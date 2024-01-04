package tech.powerjob.server.persistence.config;

import tech.powerjob.server.common.PowerJobServerConfigKey;
import tech.powerjob.server.common.utils.PropertyUtils;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * 自定义表前缀，配置项 oms.table-prefix 不配置时，不增加表前缀。
 * 参考实现：{@link org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy}
 * <p>
 * 1. 继承 PhysicalNamingStrategy 类，实现自定义表前缀；
 * </p>
 * <p>
 * 2. 修改@Query(nativeQuery = true)和其SQL，用对象名和属性名代替表名和数据库字段名。
 * </p>
 *
 * @author songyinyin
 * @since 2020/7/18
 */
public class PowerJobPhysicalNamingStrategy extends SpringPhysicalNamingStrategy implements Serializable {


    /**
     * 映射物理表名称，如：把实体表 AppInfoDO 的 DO 去掉，再加上表前缀
     *
     * @param name            实体名称
     * @param jdbcEnvironment jdbc环境变量
     * @return 映射后的物理表
     */
    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {

        String tablePrefix = PropertyUtils.getProperties().getProperty(PowerJobServerConfigKey.TABLE_PREFIX);

        String text = name.getText();
        String noDOText = StringUtils.endsWithIgnoreCase(text, "do") ? text.substring(0, text.length() - 2) : text;
        String newText = StringUtils.isNotEmpty(tablePrefix) ? tablePrefix + noDOText : noDOText;
        return super.toPhysicalTableName(new Identifier(newText, name.isQuoted()), jdbcEnvironment);
    }


}
