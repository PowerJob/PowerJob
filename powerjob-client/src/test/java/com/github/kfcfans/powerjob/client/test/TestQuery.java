package com.github.kfcfans.powerjob.client.test;

import com.alibaba.fastjson.JSON;
import com.github.kfcfans.powerjob.common.request.query.JobInfoQuery;
import com.github.kfcfans.powerjob.common.ExecuteType;
import com.github.kfcfans.powerjob.common.ProcessorType;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.response.JobInfoDTO;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

/**
 * Test the query method
 *
 * @author tjq
 * @since 1/16/21
 */
@Slf4j
class TestQuery extends ClientInitializer {

    @Test
    void testFetchAllJob() {
        ResultDTO<List<JobInfoDTO>> allJobRes = ohMyClient.fetchAllJob();
        System.out.println(JSON.toJSONString(allJobRes));
    }

    @Test
    void testQueryJob() {
        JobInfoQuery jobInfoQuery = new JobInfoQuery()
                .setIdGt(-1L)
                .setIdLt(10086L)
                .setJobNameLike("DAG")
                .setGmtModifiedGt(DateUtils.addYears(new Date(), -10))
                .setGmtCreateLt(DateUtils.addDays(new Date(), 10))
                .setExecuteTypeIn(Lists.newArrayList(ExecuteType.STANDALONE.getV(), ExecuteType.BROADCAST.getV(), ExecuteType.MAP_REDUCE.getV()))
                .setProcessorTypeIn(Lists.newArrayList(ProcessorType.EMBEDDED_JAVA.getV(), ProcessorType.SHELL.getV(), ProcessorType.JAVA_CONTAINER.getV()))
                .setProcessorInfoLike("com.github.kfcfans");

        ResultDTO<List<JobInfoDTO>> jobQueryResult = ohMyClient.queryJob(jobInfoQuery);
        System.out.println(JSON.toJSONString(jobQueryResult));
        System.out.println(jobQueryResult.getData().size());
    }
}
