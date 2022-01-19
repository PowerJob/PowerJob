package tech.powerjob.server.extension.defaultimpl.alarm.impl;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.utils.HttpUtils;
import tech.powerjob.server.extension.AlarmComponent;
import tech.powerjob.server.extension.defaultimpl.alarm.config.NeteaseAlarmConfig;
import tech.powerjob.server.extension.defaultimpl.alarm.module.Alarm;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author Echo009
 * @since 2022/1/19
 * https://wiki.mail.netease.com/pages/viewpage.action?pageId=38508962
 */
@Service
@Slf4j
public class NeteasePopoAlarmService implements AlarmComponent {

    @Resource
    private NeteaseAlarmConfig alarmConfig;

    private static final String MESSAGE_TYPE = "4";

    private static final String POPO_SERVER_ADDRESS = "http://smsserv.163.internal/mobileserv/popoPush.do";

    private static final int SUCCESS_CODE = 0;

    @Override
    public void onFailed(Alarm alarm, List<UserInfoDO> targetUserList) {

        String finalContent = alarm.fetchSimpleContent() + "From: Chronos(" + alarmConfig.getEnv() + ")" + OmsConstant.LINE_SEPARATOR
                + "Console Address: " + alarmConfig.getAddress();

        Map<String, String> params = Maps.newHashMap();
        params.put("from", alarmConfig.getPopoAccount());
        params.put("msgtype", MESSAGE_TYPE);
        params.put("content", finalContent);
        for (UserInfoDO userInfoDO : targetUserList) {
            String email = userInfoDO.getEmail();
            if (StringUtils.isBlank(email)) {
                continue;
            }
            params.put("to", email);
            String r = HttpUtils.get(POPO_SERVER_ADDRESS, params);
            if (Integer.parseInt(r) != SUCCESS_CODE) {
                log.warn("[NeteasePopoAlarm] fail to send alarm to {},rtn:{}",email,r);
            }
        }

    }
}
