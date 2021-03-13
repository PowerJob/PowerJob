package com.github.kfcfans.powerjob.client.test;

import com.github.kfcfans.powerjob.common.enums.ExecuteType;
import com.github.kfcfans.powerjob.common.enums.ProcessorType;
import com.github.kfcfans.powerjob.common.enums.TimeExpressionType;
import com.github.kfcfans.powerjob.common.request.http.SaveJobInfoRequest;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ForkJoinPool;

/**
 * TestConcurrencyControl
 *
 * @author tjq
 * @since 1/16/21
 */
class TestConcurrencyControl extends ClientInitializer {

    @Test
    void testRunJobConcurrencyControl() {

        SaveJobInfoRequest saveJobInfoRequest = new SaveJobInfoRequest();
        saveJobInfoRequest.setJobName("test concurrency control job");
        saveJobInfoRequest.setProcessorType(ProcessorType.SHELL);
        saveJobInfoRequest.setProcessorInfo("pwd");
        saveJobInfoRequest.setExecuteType(ExecuteType.STANDALONE);
        saveJobInfoRequest.setTimeExpressionType(TimeExpressionType.API);
        saveJobInfoRequest.setMaxInstanceNum(1);

        Long jobId = ohMyClient.saveJob(saveJobInfoRequest).getData();

        System.out.println("jobId: " + jobId);

        ForkJoinPool pool = new ForkJoinPool(32);

        for (int i = 0; i < 100; i++) {
            String params = "index-" + i;
            pool.execute(() -> {
                ResultDTO<Long> res = ohMyClient.runJob(jobId, params, 0);
                System.out.println(params + ": " + res);
            });
        }
    }
}
