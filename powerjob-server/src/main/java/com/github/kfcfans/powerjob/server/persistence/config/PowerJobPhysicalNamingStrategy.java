package com.github.kfcfans.powerjob.server.persistence.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Locale;

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
 * @date 2020/7/18 下午 11:01
 * @since 3.1.4
 */
@Component
public class PowerJobPhysicalNamingStrategy implements PhysicalNamingStrategy, Serializable {

    @Value("${oms.table-prefix:}")
    private String tablePrefix;

    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return apply(name, jdbcEnvironment);
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return apply(name, jdbcEnvironment);
    }

    /**
     * 映射物理表名称，如：把实体表 AppInfoDO 的 DO 去掉，再加上表前缀
     *
     * @param name            实体名称
     * @param jdbcEnvironment jdbc环境变量
     * @return 映射后的物理表
     */
    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        Identifier identifier = apply(name, jdbcEnvironment);

        String text = identifier.getText();
        String noDOText = StringUtils.endsWithIgnoreCase(text, "do") ? text.substring(0, text.length() - 2) : text;
        String newText = StringUtils.hasLength(tablePrefix) ? tablePrefix + noDOText : noDOText;
        return new Identifier(newText, identifier.isQuoted());
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return apply(name, jdbcEnvironment);
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return apply(name, jdbcEnvironment);
    }

    private Identifier apply(Identifier name, JdbcEnvironment jdbcEnvironment) {
        if (name == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(name.getText().replace('.', '_'));
        for (int i = 1; i < builder.length() - 1; i++) {
            if (isUnderscoreRequired(builder.charAt(i - 1), builder.charAt(i), builder.charAt(i + 1))) {
                builder.insert(i++, '_');
            }
        }
        return getIdentifier(builder.toString(), name.isQuoted(), jdbcEnvironment);
    }

    /**
     * Get an identifier for the specified details. By default this method will return an
     * identifier with the name adapted based on the result of
     * {@link #isCaseInsensitive(JdbcEnvironment)}
     *
     * @param name            the name of the identifier
     * @param quoted          if the identifier is quoted
     * @param jdbcEnvironment the JDBC environment
     * @return an identifier instance
     */
    protected Identifier getIdentifier(String name, boolean quoted, JdbcEnvironment jdbcEnvironment) {
        if (isCaseInsensitive(jdbcEnvironment)) {
            name = name.toLowerCase(Locale.ROOT);
        }
        return new Identifier(name, quoted);
    }

    /**
     * Specify whether the database is case sensitive.
     *
     * @param jdbcEnvironment the JDBC environment which can be used to determine case
     * @return true if the database is case insensitive sensitivity
     */
    protected boolean isCaseInsensitive(JdbcEnvironment jdbcEnvironment) {
        return true;
    }

    private boolean isUnderscoreRequired(char before, char current, char after) {
        return Character.isLowerCase(before) && Character.isUpperCase(current) && Character.isLowerCase(after);
    }
}
