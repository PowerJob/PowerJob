package tech.powerjob.client.test;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.request.query.InstanceInfoQuery;
import tech.powerjob.common.request.query.JobInfoQuery;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.response.WorkflowInstanceInfoDTO;

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
        ResultDTO<List<JobInfoDTO>> allJobRes = powerJobClient.fetchAllJob();
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
                .setProcessorTypeIn(Lists.newArrayList(ProcessorType.BUILT_IN.getV(), ProcessorType.SHELL.getV(), ProcessorType.EXTERNAL.getV()))
                .setProcessorInfoLike("tech.powerjob");

        ResultDTO<List<JobInfoDTO>> jobQueryResult = powerJobClient.queryJob(jobInfoQuery);
        System.out.println(JSON.toJSONString(jobQueryResult));
        System.out.println(jobQueryResult.getData().size());
    }

    @Test
    void testQueryInstanceInfo() {
        InstanceInfoQuery instanceInfoQuery = new InstanceInfoQuery();
        instanceInfoQuery.setJobIdEq(89L);
        ResultDTO<List<InstanceInfoDTO>> resultDTO = powerJobClient.queryInstanceInfo(instanceInfoQuery);
        List<InstanceInfoDTO> instanceInfoDTOList = resultDTO.getData();
        instanceInfoDTOList.size();
    }

    @Test
    void testQueryWorkflowInstanceInfo() {
        ResultDTO<List<WorkflowInstanceInfoDTO>> resultDTO = powerJobClient.queryWorkflowInstanceInfo(2L);
        List<WorkflowInstanceInfoDTO> workflowInstanceInfoDTOList = resultDTO.getData();
        workflowInstanceInfoDTOList.size();
    }

}
