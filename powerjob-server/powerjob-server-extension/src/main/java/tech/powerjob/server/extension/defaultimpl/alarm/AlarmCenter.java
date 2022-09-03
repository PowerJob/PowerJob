package tech.powerjob.server.extension.defaultimpl.alarm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.powerjob.server.extension.defaultimpl.alarm.module.Alarm;
import tech.powerjob.server.extension.Alarmable;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
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
@Component
public class AlarmCenter {

    private final ExecutorService POOL;
    private final List<Alarmable> BEANS = Lists.newLinkedList();

    @Autowired
    public AlarmCenter(List<Alarmable> alarmables) {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("AlarmPool-%d").build();
        POOL = new ThreadPoolExecutor(cores, cores, 5, TimeUnit.MINUTES, Queues.newLinkedBlockingQueue(), factory);

        alarmables.forEach(bean -> {
            BEANS.add(bean);
            log.info("[AlarmCenter] bean(className={},obj={}) register to AlarmCenter successfully!", bean.getClass().getName(), bean);
        });
    }

    public void alarmFailed(Alarm alarm, List<UserInfoDO> targetUserList) {
        POOL.execute(() -> BEANS.forEach(alarmable -> {
            try {
                alarmable.onFailed(alarm, targetUserList);
            }catch (Exception e) {
                log.warn("[AlarmCenter] alarm failed.", e);
            }
        }));
    }
}
