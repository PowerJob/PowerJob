package com.netease.mail.chronos.executor.support.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.netease.mail.chronos.base.exception.BaseException;
import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;
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
    public void sendNotify(SpRemindTaskInfo spRemindTaskInfo, OmsLogger omsLogger) {

        List<NotifyParamDTO> params = new ArrayList<>();

        HashMap<String, Object> originParams = JSON.parseObject(spRemindTaskInfo.getParam(), new TypeReference<HashMap<String, Object>>() {
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
        NotifyRequest.Builder builder = NotifyRequest.newBuilder();
        builder.token(generateToken(spRemindTaskInfo))
                .params(params)
                .type(MESSAGE_TYPE);
        // 处理 uid ，这次的原始 uid 有可能是 muid 或者 uid
        if (isRealUid(spRemindTaskInfo.getUid())){
            builder.uid(spRemindTaskInfo.getUid());
        }else {
            builder.muid(spRemindTaskInfo.getUid());
        }
        StatusResult statusResult = notifyClient.notifyByDomain(builder.build());

        if (statusResult.getCode() != 200) {
            omsLogger.error("处理任务(id:{},colId:{},compId:{})失败,rtn = {}", spRemindTaskInfo.getId(), spRemindTaskInfo.getColId(), spRemindTaskInfo.getCompId(), statusResult);
            throw new BaseException(statusResult.getDesc());
        }
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

    private String generateToken(SpRemindTaskInfo spRemindTaskInfo) {
        // 根据 id 和 triggerTime 生成唯一 id
        Long id = spRemindTaskInfo.getId();
        Long nextTriggerTime = spRemindTaskInfo.getNextTriggerTime();
        return id + "#" + nextTriggerTime;
    }
}
