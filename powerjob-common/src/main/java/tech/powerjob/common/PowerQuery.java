package tech.powerjob.common;

import lombok.Getter;
import lombok.Setter;

/**
 * PowerJob Query interface
 *
 * @author tjq
 * @since 2021/1/15
 */
@Getter
@Setter
public abstract class PowerQuery {

    public static String EQUAL = "Eq";

    public static String NOT_EQUAL = "NotEq";

    public static String LIKE = "Like";

    public static String NOT_LIKE = "NotLike";

    public static String LESS_THAN = "Lt";

    public static String LESS_THAN_EQUAL = "LtEq";

    public static String GREATER_THAN = "Gt";

    public static String GREATER_THAN_EQUAL = "GtEq";

    public static String IN = "In";

    public static String NOT_IN = "NotIn";

    public static String IS_NULL = "IsNull";

    public static String IS_NOT_NULL = "IsNotNull";

    private Long appIdEq;
}
