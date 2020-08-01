package com.github.kfcfans.powerjob.server.service.alarm;

import com.github.kfcfans.powerjob.server.persistence.core.model.UserInfoDO;

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
