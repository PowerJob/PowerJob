package tech.powerjob.server.core.service.impl.job;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import tech.powerjob.common.enums.DispatchStrategy;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.model.AlarmConfig;
import tech.powerjob.common.model.LifeCycle;
import tech.powerjob.common.model.LogConfig;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;

import java.util.stream.Collectors;

/**
 * JobConverter
 *
 * @author tjq
 * @since 2023/3/4
 */
public class JobConverter {

    public static SaveJobInfoRequest convertJobInfoDO2SaveJobInfoRequest(JobInfoDO jobInfoDO) {
        SaveJobInfoRequest saveJobInfoRequest = new SaveJobInfoRequest();
        BeanUtils.copyProperties(jobInfoDO, saveJobInfoRequest);
        saveJobInfoRequest.setTimeExpressionType(TimeExpressionType.of(jobInfoDO.getTimeExpressionType()));
        saveJobInfoRequest.setExecuteType(ExecuteType.of(jobInfoDO.getExecuteType()));
        saveJobInfoRequest.setProcessorType(ProcessorType.of(jobInfoDO.getProcessorType()));
        if (StringUtils.isNotEmpty(jobInfoDO.getNotifyUserIds())) {
            saveJobInfoRequest.setNotifyUserIds(Lists.newArrayList(SJ.COMMA_SPLITTER.split(jobInfoDO.getNotifyUserIds())).stream().map(Long::valueOf).collect(Collectors.toList()));
        }
        saveJobInfoRequest.setDispatchStrategy(DispatchStrategy.of(jobInfoDO.getDispatchStrategy()));
        saveJobInfoRequest.setLifeCycle(LifeCycle.parse(jobInfoDO.getLifecycle()));
        saveJobInfoRequest.setAlarmConfig(JsonUtils.parseObjectIgnoreException(jobInfoDO.getAlarmConfig(), AlarmConfig.class));
        saveJobInfoRequest.setLogConfig(JsonUtils.parseObjectIgnoreException(jobInfoDO.getLogConfig(), LogConfig.class));
        return saveJobInfoRequest;
    }

    public static JobInfoDTO convertJobInfoDO2JobInfoDTO(JobInfoDO jobInfoDO) {
        JobInfoDTO jobInfoDTO = new JobInfoDTO();
        BeanUtils.copyProperties(jobInfoDO, jobInfoDTO);
        if (jobInfoDO.getAlarmConfig() != null) {
            jobInfoDTO.setAlarmConfig(JSON.parseObject(jobInfoDO.getAlarmConfig(), AlarmConfig.class));
        }
        return jobInfoDTO;
    }

}
