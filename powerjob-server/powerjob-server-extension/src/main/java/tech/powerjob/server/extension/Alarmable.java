package tech.powerjob.server.extension;

import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.extension.defaultimpl.alram.module.Alarm;

import java.util.List;

/**
 * 报警接口
 *
 * @author tjq
 * @since 2020/4/19
 */
public interface Alarmable {

    void onFailed(Alarm alarm, List<UserInfoDO> targetUserList);
}
