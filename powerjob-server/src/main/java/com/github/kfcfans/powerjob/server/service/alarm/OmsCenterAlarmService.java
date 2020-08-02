package com.github.kfcfans.powerjob.server.service.alarm;

import com.github.kfcfans.powerjob.server.common.PowerJobServerConfigKey;
import com.github.kfcfans.powerjob.server.common.SJ;
import com.github.kfcfans.powerjob.server.common.utils.SpringUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.UserInfoDO;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 报警服务
 *
 * @author tjq
 * @since 2020/4/19
 */
@Slf4j
@Service("omsCenterAlarmService")
public class OmsCenterAlarmService implements Alarmable {

    @Resource
    private Environment environment;

    private List<Alarmable> alarmableList;
    private volatile boolean initialized = false;

    @Async("omsCommonPool")
    @Override
    public void onFailed(Alarm alarm, List<UserInfoDO> targetUserList) {
        init();
        alarmableList.forEach(alarmable -> {
            try {
                alarmable.onFailed(alarm, targetUserList);
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
            String beanNames = environment.getProperty(PowerJobServerConfigKey.ALARM_BEAN_NAMES);
            if (StringUtils.isNotEmpty(beanNames)) {
                SJ.commaSplitter.split(beanNames).forEach(beanName -> {
                    try {
                        Alarmable bean = (Alarmable) SpringUtils.getBean(beanName);
                        alarmableList.add(bean);
                        log.info("[OmsCenterAlarmService] load Alarmable for bean: {} successfully.", beanName);
                    }catch (Exception e) {
                        log.warn("[OmsCenterAlarmService] initialize Alarmable for bean: {} failed.", beanName, e);
                    }
                });
            }
            initialized = true;
        }
    }

}
