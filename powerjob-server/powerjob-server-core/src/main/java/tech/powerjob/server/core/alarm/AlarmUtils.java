package tech.powerjob.server.core.alarm;

import org.springframework.beans.BeanUtils;
import tech.powerjob.common.utils.CollectionUtils;
import tech.powerjob.server.extension.alarm.AlarmTarget;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AlarmUtils
 *
 * @author tjq
 * @since 2023/7/31
 */
public class AlarmUtils {

    public static List<AlarmTarget> convertUserInfoList2AlarmTargetList(List<UserInfoDO> userInfoDOS) {
        if (CollectionUtils.isEmpty(userInfoDOS)) {
            return Collections.emptyList();
        }
        return userInfoDOS.stream().map(AlarmUtils::convertUserInfo2AlarmTarget).collect(Collectors.toList());
    }

    public static AlarmTarget convertUserInfo2AlarmTarget(UserInfoDO userInfoDO) {
        AlarmTarget alarmTarget = new AlarmTarget();
        BeanUtils.copyProperties(userInfoDO, alarmTarget);

        alarmTarget.setName(userInfoDO.getUsername());
        return alarmTarget;
    }

}
