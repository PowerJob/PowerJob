package tech.powerjob.server.core.alarm;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import tech.powerjob.server.extension.alarm.Alarm;
import tech.powerjob.server.extension.alarm.AlarmTarget;
import tech.powerjob.server.extension.alarm.Alarmable;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
                alarmable.onFailed(alarm, targetUserList.stream().map(AlarmCenter::convertUserInfo2AlarmTarget).collect(Collectors.toList()));
            }catch (Exception e) {
                log.warn("[AlarmCenter] alarm failed.", e);
            }
        }));
    }

    private static AlarmTarget convertUserInfo2AlarmTarget(UserInfoDO userInfoDO) {
        AlarmTarget alarmTarget = new AlarmTarget();
        BeanUtils.copyProperties(userInfoDO, alarmTarget);

        alarmTarget.setName(userInfoDO.getUsername());
        return alarmTarget;
    }
}
