package tech.powerjob.server.persistence.config;

import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.Types;

/**
 * @author Kung Yao
 * 2021/3/24 下午 04:23
 * 1074_King
 */
public class PowerJobPGDialect extends PostgreSQL10Dialect {

    @Override
    public SqlTypeDescriptor remapSqlTypeDescriptor( SqlTypeDescriptor sqlTypeDescriptor ) {
        if ( Types.CLOB == sqlTypeDescriptor.getSqlType() ) {
            return LongVarcharTypeDescriptor.INSTANCE;
        }
        return super.remapSqlTypeDescriptor( sqlTypeDescriptor );
    }
}
