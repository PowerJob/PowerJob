package tech.powerjob.client.test;

import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.ResultDTO;
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

        Long jobId = powerJobClient.saveJob(saveJobInfoRequest).getData();

        System.out.println("jobId: " + jobId);

        ForkJoinPool pool = new ForkJoinPool(32);

        for (int i = 0; i < 100; i++) {
            String params = "index-" + i;
            pool.execute(() -> {
                ResultDTO<Long> res = powerJobClient.runJob(jobId, params, 0);
                System.out.println(params + ": " + res);
            });
        }
    }
}
