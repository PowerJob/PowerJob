package com.netease.mail.chronos.base.utils;

import cn.hutool.core.bean.BeanUtil;
import lombok.SneakyThrows;
import lombok.val;

/**
 * @author Echo009
 * @since 2021/9/23
 */
public class SimpleBeanConvertUtil {

    private SimpleBeanConvertUtil() {

    }

    /**
     * 需要确保 目标类型 具有无参构造方法
     */
    @SneakyThrows
    public static <S, T> T convert(S source, Class<T> clazz) {
        val t = clazz.newInstance();
        BeanUtil.copyProperties(source, t, true);
        return t;
    }


}
