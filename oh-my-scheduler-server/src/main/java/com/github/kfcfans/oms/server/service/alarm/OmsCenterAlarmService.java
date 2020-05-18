package com.github.kfcfans.oms.server.service.alarm;

import com.github.kfcfans.oms.server.common.utils.SpringUtils;
import com.github.kfcfans.oms.server.persistence.core.model.UserInfoDO;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 报警服务
 *
 * @author tjq
 * @since 2020/4/19
 */
@Slf4j
@Service("omsCenterAlarmService")
public class OmsCenterAlarmService implements Alarmable {

    @Setter
    @Value("${oms.alarm.bean.names}")
    private String beanNames;

    private List<Alarmable> alarmableList;
    private volatile boolean initialized = false;

    public OmsCenterAlarmService() {
    }

    @Async("omsCommonPool")
    @Override
    public void alarm(AlarmContent alarmContent, List<UserInfoDO> targetUserList) {
        init();
        alarmableList.forEach(alarmable -> {
            try {
                alarmable.alarm(alarmContent, targetUserList);
            }catch (Exception e) {
                log.warn("[OmsCenterAlarmService] alarm failed.", e);
            }
        });
    }

    /**
     * 初始化
     * 使用 InitializingBean 进行初始化会导致 NPE，因为没办法控制Bean（开发者自己实现的Bean）的加载顺序
     */
    private void init() {

        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }

            alarmableList = Lists.newLinkedList();
            Splitter.on(",").split(beanNames).forEach(beanName -> {
                try {
                    Alarmable bean = (Alarmable) SpringUtils.getBean(beanName);
                    alarmableList.add(bean);
                    log.info("[OmsCenterAlarmService] load Alarmable for bean: {} successfully.", beanName);
                }catch (Exception e) {
                    log.warn("[OmsCenterAlarmService] initialize Alarmable for bean: {} failed.", beanName, e);
                }
            });

            initialized = true;
        }
    }
}
