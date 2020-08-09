package com.github.kfcfans.powerjob.server.service.alarm;

import com.github.kfcfans.powerjob.server.persistence.core.model.UserInfoDO;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;

/**
 * 报警服务
 *
 * @author tjq
 * @since 2020/4/19
 */
@Slf4j
public class AlarmCenter {

    private static final ExecutorService POOL;
    private static final List<Alarmable> BEANS = Lists.newLinkedList();
    private static final int THREAD_KEEP_ALIVE_TIME_M = 5;

    static {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("AlarmPool-%d").build();
        POOL = new ThreadPoolExecutor(cores, cores, THREAD_KEEP_ALIVE_TIME_M, TimeUnit.MINUTES, Queues.newLinkedBlockingQueue(), factory);
    }


    public static void alarmFailed(Alarm alarm, List<UserInfoDO> targetUserList) {
        POOL.execute(() -> BEANS.forEach(alarmable -> {
            try {
                alarmable.onFailed(alarm, targetUserList);
            }catch (Exception e) {
                log.warn("[AlarmCenter] alarm failed.", e);
            }
        }));
    }

    public static void register(Alarmable alarmable) {
        BEANS.add(alarmable);
        log.info("[AlarmCenter] bean(className={},obj={}) register to AlarmCenter successfully!", alarmable.getClass().getName(), alarmable);
    }
}
