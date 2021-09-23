package com.netease.mail.chronos.base.utils;


import com.netease.mail.chronos.base.enums.CodeEnum;

/**
 * @author Echo009
 * @since 2021/7/9
 */
public final class EnumUtil {

    private EnumUtil() {

    }

    /**
     * 通过 code 获取对应的枚举
     *
     * @param code  code
     * @param clazz 枚举类
     * @return 枚举类
     */
    public static <A, B extends CodeEnum<A>> B getEnumByCode(Object code, Class<B> clazz) {
        for (B item : clazz.getEnumConstants()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 通过 code 获取对应的枚举
     *
     * @param code  code
     * @param clazz 枚举类
     * @param defaultValue 默认值
     * @return 枚举类
     */
    public static <A, B extends CodeEnum<A>> B getEnumByCodeOrDefault(Object code, Class<B> clazz, B defaultValue) {
        B res = getEnumByCode(code, clazz);
        return res == null ? defaultValue : res;
    }


}
