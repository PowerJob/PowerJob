package tech.powerjob.client.test;

import com.alibaba.fastjson.JSONObject;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.request.http.SaveWorkflowNodeRequest;
import tech.powerjob.common.request.http.SaveWorkflowRequest;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.common.response.WorkflowInfoDTO;
import tech.powerjob.common.response.WorkflowInstanceInfoDTO;
import tech.powerjob.common.response.WorkflowNodeInfoDTO;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Test cases for {@link PowerJobClient} workflow.
 *
 * @author tjq
 * @author Echo009
 * @since 2020/6/2
 */
class TestWorkflow extends ClientInitializer {

    private static final long WF_ID = 1;

    @Test
    void initTestData() {
        SaveJobInfoRequest base = new SaveJobInfoRequest();
        base.setJobName("DAG-Node-");
        base.setTimeExpressionType(TimeExpressionType.WORKFLOW);
        base.setExecuteType(ExecuteType.STANDALONE);
        base.setProcessorType(ProcessorType.BUILT_IN);
        base.setProcessorInfo("tech.powerjob.samples.workflow.WorkflowStandaloneProcessor");

        for (int i = 0; i < 5; i++) {
            SaveJobInfoRequest request = JSONObject.parseObject(JSONObject.toJSONBytes(base), SaveJobInfoRequest.class);
            request.setJobName(request.getJobName() + i);
            ResultDTO<Long> res = powerJobClient.saveJob(request);
            System.out.println(res);
            Assertions.assertNotNull(res);

        }
    }

    @Test
    void testSaveWorkflow() {

        SaveWorkflowRequest req = new SaveWorkflowRequest();

        req.setWfName("workflow-by-client");
        req.setWfDescription("created by client");
        req.setEnable(true);
        req.setTimeExpressionType(TimeExpressionType.API);

        System.out.println("req ->" + JSONObject.toJSON(req));
        ResultDTO<Long> res = powerJobClient.saveWorkflow(req);
        System.out.println(res);
        Assertions.assertNotNull(res);

        req.setId(res.getData());

        // 创建节点
        SaveWorkflowNodeRequest saveWorkflowNodeRequest1 = new SaveWorkflowNodeRequest();
        saveWorkflowNodeRequest1.setJobId(1L);
        saveWorkflowNodeRequest1.setNodeName("DAG-Node-1");
        saveWorkflowNodeRequest1.setType(WorkflowNodeType.JOB.getCode());

        SaveWorkflowNodeRequest saveWorkflowNodeRequest2 = new SaveWorkflowNodeRequest();
        saveWorkflowNodeRequest2.setJobId(1L);
        saveWorkflowNodeRequest2.setNodeName("DAG-Node-2");
        saveWorkflowNodeRequest2.setType(WorkflowNodeType.JOB.getCode());


        SaveWorkflowNodeRequest saveWorkflowNodeRequest3 = new SaveWorkflowNodeRequest();
        saveWorkflowNodeRequest3.setJobId(1L);
        saveWorkflowNodeRequest3.setNodeName("DAG-Node-3");
        saveWorkflowNodeRequest3.setType(WorkflowNodeType.JOB.getCode());


        List<WorkflowNodeInfoDTO> nodeList = powerJobClient.saveWorkflowNode(Lists.newArrayList(saveWorkflowNodeRequest1,saveWorkflowNodeRequest2,saveWorkflowNodeRequest3)).getData();
        System.out.println(nodeList);
        Assertions.assertNotNull(nodeList);


        // DAG 图
        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        nodes.add(new PEWorkflowDAG.Node(nodeList.get(0).getId()));
        nodes.add(new PEWorkflowDAG.Node(nodeList.get(1).getId()));
        nodes.add(new PEWorkflowDAG.Node(nodeList.get(2).getId()));

        edges.add(new PEWorkflowDAG.Edge(nodeList.get(0).getId(), nodeList.get(1).getId()));
        edges.add(new PEWorkflowDAG.Edge(nodeList.get(1).getId(), nodeList.get(2).getId()));
        PEWorkflowDAG peWorkflowDAG = new PEWorkflowDAG(nodes, edges);

        // 保存完整信息
        req.setDag(peWorkflowDAG);
        res = powerJobClient.saveWorkflow(req);

        System.out.println(res);
        Assertions.assertNotNull(res);

    }

    @Test
    void testCopyWorkflow() {
        ResultDTO<Long> res = powerJobClient.copyWorkflow(WF_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }


    @Test
    void testDisableWorkflow() {
        ResultDTO<Void> res = powerJobClient.disableWorkflow(WF_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testDeleteWorkflow() {
        ResultDTO<Void> res = powerJobClient.deleteWorkflow(WF_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testEnableWorkflow() {
        ResultDTO<Void> res = powerJobClient.enableWorkflow(WF_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testFetchWorkflowInfo() {
        ResultDTO<WorkflowInfoDTO> res = powerJobClient.fetchWorkflow(WF_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testRunWorkflow() {
        ResultDTO<Long> res = powerJobClient.runWorkflow(WF_ID, null, 0);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testStopWorkflowInstance() {
        ResultDTO<Void> res = powerJobClient.stopWorkflowInstance(149962433421639744L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testRetryWorkflowInstance() {
        ResultDTO<Void> res = powerJobClient.retryWorkflowInstance(149962433421639744L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testMarkWorkflowNodeAsSuccess() {
        ResultDTO<Void> res = powerJobClient.markWorkflowNodeAsSuccess(149962433421639744L, 1L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testFetchWfInstanceInfo() {
        ResultDTO<WorkflowInstanceInfoDTO> res = powerJobClient.fetchWorkflowInstanceInfo(149962433421639744L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testRunWorkflowPlus() {
        ResultDTO<Long> res = powerJobClient.runWorkflow(WF_ID, "this is init Params 2", 90000);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }
}
