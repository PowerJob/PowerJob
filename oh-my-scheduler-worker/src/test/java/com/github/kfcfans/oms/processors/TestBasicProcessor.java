package com.github.kfcfans.oms.processors;

import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.worker.core.processor.ProcessResult;
import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor;

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
        System.out.println("TaskContext: " + JsonUtils.toJSONString(context) + ";time = " + System.currentTimeMillis());
        return new ProcessResult(true, System.currentTimeMillis() + "success");
    }


}
