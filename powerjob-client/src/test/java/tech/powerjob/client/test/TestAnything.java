package tech.powerjob.client.test;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tech.powerjob.client.test.utils.TestUtils;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.utils.CommonUtils;

/**
 * TestAnything
 *
 * @author tjq
 * @since 2024/8/19
 */
@Slf4j
public class TestAnything extends ClientInitializer {

    @Test
    void testMultiClientRequest() {
        for (int i = 0; i < 5; i++) {
            log.info("START ================== {} ================== START", i);

            SaveJobInfoRequest saveJobInfoRequest = TestUtils.newSaveJobRequest(null, "TEST-JOB-" + i + "-" + System.currentTimeMillis());
            ResultDTO<Long> saveJobResult = powerJobClient.saveJob(saveJobInfoRequest);
            log.info("[testMultiClientRequest] saveJobResult: {}", JSONObject.toJSONString(saveJobResult));

            log.info("END ================== {} ================== END", i);

            CommonUtils.easySleep(100);
        }
    }

}
