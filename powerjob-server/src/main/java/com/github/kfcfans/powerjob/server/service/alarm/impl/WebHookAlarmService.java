package com.github.kfcfans.powerjob.server.service.alarm.impl;

import com.github.kfcfans.powerjob.common.utils.HttpUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.UserInfoDO;
import com.github.kfcfans.powerjob.server.service.alarm.Alarm;
import com.github.kfcfans.powerjob.server.service.alarm.Alarmable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * http 回调报警
 *
 * @author tjq
 * @since 11/14/20
 */
@Slf4j
@Service
public class WebHookAlarmService implements Alarmable {

    @Override
    public void onFailed(Alarm alarm, List<UserInfoDO> targetUserList) {
        if (CollectionUtils.isEmpty(targetUserList)) {
            return;
        }
        targetUserList.forEach(user -> {
            String webHook = user.getWebHook();
            if (StringUtils.isEmpty(webHook)) {
                return;
            }
            try {
                String response = HttpUtils.get(webHook);
                log.info("[WebHookAlarmService] invoke webhook[url={}] successfully, response is {}", webHook, response);
            }catch (Exception e) {
                log.warn("[WebHookAlarmService] invoke webhook[url={}] failed!", webHook, e);
            }
        });
    }
}
