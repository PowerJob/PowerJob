package com.netease.mail.chronos.portal.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
 * @author Echo009
 * @since 2021/8/26
 */
public class QueryWrapperUtil {

    private QueryWrapperUtil() {

    }

    public static <T> QueryWrapper<T> construct(String c1, Object v1) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.eq(c1, v1);
        return wrapper;
    }


    public static <T> QueryWrapper<T> construct(String c1, Object v1, String c2, Object v2) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.eq(c1, v1);
        wrapper.eq(c2, v2);
        return wrapper;
    }
}
