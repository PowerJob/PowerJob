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

/**
 * 报警服务
 *
 * @author tjq
 * @since 2020/4/19
 */
@Slf4j
@Service("omsCenterAlarmService")
public class OmsCenterAlarmService implements Alarmable, InitializingBean {

    @Setter
    @Value("${oms.alarm.bean.names}")
    private String beanNames;

    private List<Alarmable> alarmableList;

    public OmsCenterAlarmService() {
    }

    @Async("omsCommonPool")
    @Override
    public void alarm(AlarmContent alarmContent, List<UserInfoDO> targetUserList) {
        alarmableList.forEach(alarmable -> {
            try {
                alarmable.alarm(alarmContent, targetUserList);
            }catch (Exception e) {
                log.warn("[OmsCenterAlarmService] alarm failed.", e);
            }
        });
    }


    @Override
    public void afterPropertiesSet() throws Exception {
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
    }
}
