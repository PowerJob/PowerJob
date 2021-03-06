package tech.powerjob.server.extension;

import tech.powerjob.server.persistence.core.model.UserInfoDO;
import tech.powerjob.server.service.alarm.Alarm;
import tech.powerjob.server.service.alarm.AlarmCenter;
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
