package com.netease.mail.chronos.executor.support;

import tech.powerjob.common.exception.PowerJobCheckedException;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.MapProcessor;

import java.util.List;

/**
 * @author Echo009
 * @since 2021/9/24
 */
public class RemindTaskProcessor implements MapProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {



        return null;
    }


    @Override
    public void map(List<?> taskList, String taskName) throws PowerJobCheckedException {





    }

}
