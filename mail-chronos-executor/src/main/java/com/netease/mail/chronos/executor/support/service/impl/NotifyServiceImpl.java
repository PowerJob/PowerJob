package com.netease.mail.chronos.executor.support.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.netease.mail.chronos.base.exception.BaseException;
import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;
import com.netease.mail.chronos.executor.support.entity.SpRtTaskInstance;
import com.netease.mail.chronos.executor.support.service.NotifyService;
import com.netease.mail.mp.api.notify.client.NotifyClient;
import com.netease.mail.mp.api.notify.dto.NotifyRequest;
import com.netease.mail.mp.notify.common.dto.NotifyParamDTO;
import com.netease.mail.quark.status.StatusResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.worker.log.OmsLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Echo009
 * @since 2021/10/21
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final NotifyClient notifyClient;

    private static final int MESSAGE_TYPE = 213;


    @Override
    public boolean sendNotify(SpRtTaskInstance spRtTaskInstance, OmsLogger omsLogger) {
        List<NotifyParamDTO> params = new ArrayList<>();
        HashMap<String, Object> originParams = JSON.parseObject(spRtTaskInstance.getParam(), new TypeReference<HashMap<String, Object>>() {
        });
        originParams.forEach((key, value) -> {
            NotifyParamDTO param;
            if (value instanceof String) {
                param = new NotifyParamDTO(key, (String) value);
                param.setJson(false);
            } else {
                param = new NotifyParamDTO(key, JSON.toJSONString(value));
                // 这里不得不这么判断一下，对方用的是 parseObject 方法
                param.setJson(isValidateJsonObjectString(param.getValue()));
            }
            params.add(param);
        });
        // 传递 expectedTriggerTime
        NotifyParamDTO triggerTime = new NotifyParamDTO("expectedTriggerTime", String.valueOf(spRtTaskInstance.getExpectedTriggerTime()));
        params.add(triggerTime);

        NotifyRequest.Builder builder = NotifyRequest.newBuilder();
        builder.token(generateToken(spRtTaskInstance))
                .params(params)
                .type(MESSAGE_TYPE);
        // 处理 uid ，这次的原始 uid 有可能是 muid 或者 uid
        if (isRealUid(spRtTaskInstance.getCustomKey())){
            builder.uid(spRtTaskInstance.getCustomKey());
        }else {
            builder.muid(spRtTaskInstance.getCustomKey());
        }
        StatusResult statusResult = notifyClient.notifyByDomain(builder.build());
        // 记录结果
        spRtTaskInstance.setResult(JSON.toJSONString(statusResult));
        if (statusResult.getCode() != 200) {
            log.error("处理提醒任务实例(id:{},compId:{},uid:{})失败,rtn = {}", spRtTaskInstance.getId(), spRtTaskInstance.getCustomId(), spRtTaskInstance.getCustomKey(), statusResult);
            return false;
        }
        log.info("处理提醒任务实例(id:{},compId:{},uid:{})成功,rtn = {}", spRtTaskInstance.getId(), spRtTaskInstance.getCustomId(), spRtTaskInstance.getCustomKey(), statusResult);
        return true;
    }

    private boolean isRealUid(String uid){
        return StringUtils.isNotBlank(uid) && uid.contains("@");
    }

    private boolean isValidateJsonObjectString(String value) {
        try {
            JSON.parseObject(value);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    private String generateToken(SpRtTaskInstance spRtTaskInstance) {
        // 根据 id 和 triggerTime 生成唯一 id
        Long id = spRtTaskInstance.getId();
        Long nextTriggerTime = spRtTaskInstance.getExpectedTriggerTime();
        return id + "#" + nextTriggerTime;
    }
}
