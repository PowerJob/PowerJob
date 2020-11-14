package com.github.kfcfans.powerjob.server.service.alarm.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.common.OmsConstant;
import com.github.kfcfans.powerjob.common.utils.HttpUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.UserInfoDO;
import com.github.kfcfans.powerjob.server.service.alarm.Alarm;
import com.github.kfcfans.powerjob.server.service.alarm.Alarmable;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
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

            MediaType jsonType = MediaType.parse(OmsConstant.JSON_MEDIA_TYPE);
            RequestBody requestBody = RequestBody.create(jsonType, JSONObject.toJSONString(alarm));

            try {
                String response = HttpUtils.post(webHook, requestBody);
                log.info("[WebHookAlarmService] invoke webhook[url={}] successfully, response is {}", webHook, response);
            }catch (Exception e) {
                log.warn("[WebHookAlarmService] invoke webhook[url={}] failed!", webHook, e);
            }
        });
    }
}
