package tech.powerjob.server.persistence.config.dialect;

import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.type.descriptor.sql.LongVarbinaryTypeDescriptor;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.Types;

/**
 * <a href="https://github.com/PowerJob/PowerJob/issues/750">PG数据库方言</a>
 * 使用方自行通过配置文件激活：spring.datasource.remote.hibernate.properties.hibernate.dialect=tech.powerjob.server.persistence.config.dialect.AdpPostgreSQLDialect
 *
 * @author litong0531
 * @since 2024/8/11
 */
public class AdpPostgreSQLDialect extends PostgreSQL10Dialect {

    public AdpPostgreSQLDialect() {
        super();
        registerColumnType(Types.BLOB, "bytea");
        registerColumnType(Types.CLOB, "text");
    }

    @Override
    public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
        switch (sqlTypeDescriptor.getSqlType()) {
            case Types.CLOB:
                return LongVarcharTypeDescriptor.INSTANCE;
            case Types.BLOB:
                return LongVarbinaryTypeDescriptor.INSTANCE;
            case Types.NCLOB:
                return LongVarbinaryTypeDescriptor.INSTANCE;
        }
        return super.remapSqlTypeDescriptor(sqlTypeDescriptor);
    }
}
