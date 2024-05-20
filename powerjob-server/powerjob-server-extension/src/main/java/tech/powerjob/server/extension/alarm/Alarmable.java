package tech.powerjob.server.extension.alarm;

import java.util.List;

/**
 * 报警接口
 *
 * @author tjq
 * @since 2020/4/19
 */
public interface Alarmable {

    void onFailed(Alarm alarm, List<AlarmTarget> alarmTargets);
}
