package com.github.kfcfans.oms.processors;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.ProcessorType;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.common.utils.NetUtils;
import com.github.kfcfans.oms.worker.sdk.ProcessResult;
import com.github.kfcfans.oms.worker.sdk.TaskContext;
import com.github.kfcfans.oms.worker.sdk.api.BasicProcessor;
import com.google.common.collect.Lists;

/**
 * 测试用的基础处理器
 *
 * @author tjq
 * @since 2020/3/24
 */
public class TestBasicProcessor implements BasicProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        System.out.println("======== BasicProcessor#process ========");
        System.out.println("TaskContext: " + JSONObject.toJSONString(context) + ";time = " + System.currentTimeMillis());
        return new ProcessResult(true, System.currentTimeMillis() + "success");
    }


}
