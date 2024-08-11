package tech.powerjob.client.test;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.utils.CommonUtils;

/**
 * 测试容灾能力
 *
 * @author tjq
 * @since 2024/8/11
 */
@Slf4j
public class TestClusterHA extends ClientInitializer {

    @Test
    void testHa() {
        // 人工让 server 启停
        for (int i = 0; i < 1000000; i++) {

            CommonUtils.easySleep(100);

            ResultDTO<JobInfoDTO> jobInfoDTOResultDTO = powerJobClient.fetchJob(1L);

            log.info("[TestClusterHA] response: {}", JSONObject.toJSONString(jobInfoDTOResultDTO));

            if (!jobInfoDTOResultDTO.isSuccess()) {
                throw new RuntimeException("request failed!");
            }
        }
    }
}
