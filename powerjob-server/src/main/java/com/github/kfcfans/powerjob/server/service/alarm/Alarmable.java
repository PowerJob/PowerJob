package com.github.kfcfans.powerjob.server.service.alarm;

import com.github.kfcfans.powerjob.server.persistence.core.model.UserInfoDO;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * 报警接口
 *
 * @author tjq
 * @since 2020/4/19
 */
public interface Alarmable extends InitializingBean {

    void onFailed(Alarm alarm, List<UserInfoDO> targetUserList);

    @Override
    default void afterPropertiesSet() throws Exception {
        AlarmCenter.register(this);
    }
}
