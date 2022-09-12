package tech.powerjob.server.test;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.PowerQuery;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.server.core.service.JobService;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.util.Lists;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

import java.util.Date;
import java.util.List;

/**
 * test QueryConvertUtils
 *
 * @author tjq
 * @since 2021/1/16
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class QueryConvertUtilsTest {

    @Resource
    private JobService jobService;

    @Test
    public void autoConvert() {
        JobInfoQuery jobInfoQuery = new JobInfoQuery();
        jobInfoQuery.setAppIdEq(1L);
        jobInfoQuery.setJobNameLike("DAG");
        jobInfoQuery.setStatusIn(Lists.newArrayList(1));
        jobInfoQuery.setGmtCreateGt(DateUtils.addDays(new Date(), -300));

        List<JobInfoDTO> list = jobService.queryJob(jobInfoQuery);
        System.out.println("size: " + list.size());
        System.out.println(JSONObject.toJSONString(list));
    }

    @Getter
    @Setter
    public static class JobInfoQuery extends PowerQuery {
        private String jobNameLike;
        private Date gmtCreateGt;
        private List<Integer> statusIn;
    }
}