package com.netease.mail.chronos.base.utils;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;


import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * @author Echo009
 * @since 2021/9/28
 */
class CronUtilTest {

    @SneakyThrows
    @Test
    void test() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse("2021-09-28 12:00:00");
        long nextTriggerTime = CronUtil.calculateNextTriggerTime("0 0 13/4 * * ? ", "Asia/Shanghai", date.getTime());
        System.out.println(new Date(nextTriggerTime));
    }

}