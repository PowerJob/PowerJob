package tech.powerjob.server.persistence.config.dialect;

import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.type.descriptor.sql.LongVarbinaryTypeDescriptor;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.Types;

/**
 * 拷贝自AdpPostgreSQLDialect，用来处理OpenGauss相关数据类型发言
 * 使用方自行通过配置文件激活：spring.datasource.remote.hibernate.properties.hibernate.dialect=tech.powerjob.server.persistence.config.dialect.AdpOpenGaussSQLDialect
 *
 * @since 2024/11/12
 */
public class AdpOpenGaussSQLDialect extends PostgreSQL10Dialect {

    public AdpOpenGaussSQLDialect() {
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
