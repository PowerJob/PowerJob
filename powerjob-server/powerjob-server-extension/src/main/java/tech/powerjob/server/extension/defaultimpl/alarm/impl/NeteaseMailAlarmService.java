package tech.powerjob.server.extension.defaultimpl.alarm.impl;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson.JSON;
import com.netease.mail.mp.api.notify.client.NotifyClient;
import com.netease.mail.mp.notify.common.dto.NotifyParamDTO;
import com.netease.mail.quark.status.StatusResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.server.extension.AlarmComponent;
import tech.powerjob.server.extension.defaultimpl.alarm.config.NeteaseAlarmConfig;
import tech.powerjob.server.extension.defaultimpl.alarm.module.Alarm;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Echo009
 * @since 2022/1/18
 */
@Slf4j
@Service
public class NeteaseMailAlarmService implements AlarmComponent {

    @Resource
    private  NotifyClient notifyClient;

    @Resource
    private NeteaseAlarmConfig alarmConfig;

    private static final int MESSAGE_TYPE = 267;

    @SuppressWarnings("rawtypes")
    @Override
    public void onFailed(Alarm alarm, List<UserInfoDO> targetUserList) {
        List<NotifyParamDTO> params = new ArrayList<>();
        String title = alarm.fetchTitle();
        params.add(new NotifyParamDTO("title",title));
        Map<String, String> contentMap = alarm.fetchContentMap();
        List<Item> itemList = new ArrayList<>(contentMap.size());
        for (Map.Entry<String, String> entry : contentMap.entrySet()) {
            itemList.add(new Item(entry.getKey(),entry.getValue()));
        }
        params.add(new NotifyParamDTO("content", JSON.toJSONString(itemList)));
        params.add(new NotifyParamDTO("env", alarmConfig.getEnv()));
        params.add(new NotifyParamDTO("chronosAddr", alarmConfig.getAddress()));
        for (UserInfoDO userInfoDO : targetUserList) {
            String email = userInfoDO.getEmail();
            if (StringUtils.isBlank(email)) {
                continue;
            }
            String token = UUID.fastUUID().toString();
            StatusResult statusResult = notifyClient.notifyByDomain(MESSAGE_TYPE, token, params, email);
            if (statusResult.getCode() != 200){
                log.warn("[NeteaseMailAlarm] fail to send alarm to {},rtn:{}",email,JSON.toJSONString(statusResult));
            }
        }
    }



    @AllArgsConstructor
    @Data
    public static class Item{

        private String key;

        private String value;

    }


}
