package com.github.kfcfans.powerjob.client.test;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.client.OhMyClient;
import com.github.kfcfans.powerjob.common.ExecuteType;
import com.github.kfcfans.powerjob.common.ProcessorType;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.model.PEWorkflowDAG;
import com.github.kfcfans.powerjob.common.request.http.*;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.common.response.WorkflowInfoDTO;
import com.github.kfcfans.powerjob.common.response.WorkflowInstanceInfoDTO;
import com.github.kfcfans.powerjob.common.response.WorkflowNodeInfoDTO;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Test cases for {@link OhMyClient} workflow.
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
        base.setProcessorType(ProcessorType.EMBEDDED_JAVA);
        base.setProcessorInfo("com.github.kfcfans.powerjob.samples.workflow.WorkflowStandaloneProcessor");

        for (int i = 0; i < 5; i++) {
            SaveJobInfoRequest request = JSONObject.parseObject(JSONObject.toJSONBytes(base), SaveJobInfoRequest.class);
            request.setJobName(request.getJobName() + i);
            ResultDTO<Long> res = ohMyClient.saveJob(request);
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
        ResultDTO<Long> res = ohMyClient.saveWorkflow(req);
        System.out.println(res);
        Assertions.assertNotNull(res);

    }

    @Test
    void testCopyWorkflow() {
        ResultDTO<Long> res = ohMyClient.copyWorkflow(WF_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testAddWorkflowNode() {
        AddWorkflowNodeRequest addWorkflowNodeRequest = new AddWorkflowNodeRequest();
        addWorkflowNodeRequest.setJobId(1L);
        addWorkflowNodeRequest.setWorkflowId(WF_ID);
        ResultDTO<List<WorkflowNodeInfoDTO>> res = ohMyClient.addWorkflowNode(Lists.newArrayList(addWorkflowNodeRequest));
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testModifyWorkflowNode() {
        ModifyWorkflowNodeRequest modifyWorkflowNodeRequest = new ModifyWorkflowNodeRequest();
        modifyWorkflowNodeRequest.setWorkflowId(WF_ID);
        modifyWorkflowNodeRequest.setId(1L);
        modifyWorkflowNodeRequest.setNodeAlias("(๑•̀ㅂ•́)و✧");
        modifyWorkflowNodeRequest.setEnable(false);
        modifyWorkflowNodeRequest.setSkipWhenFailed(false);
        ResultDTO<Void> res = ohMyClient.modifyWorkflowNode(modifyWorkflowNodeRequest);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testSaveWorkflowDag() {
        // DAG 图
        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        nodes.add(new PEWorkflowDAG.Node(1L, 1L, "DAG-Node-1"));
        nodes.add(new PEWorkflowDAG.Node(2L, 1L, "DAG-Node-2"));
        nodes.add(new PEWorkflowDAG.Node(3L, 1L, "DAG-Node-3"));

        edges.add(new PEWorkflowDAG.Edge(1L, 2L));
        edges.add(new PEWorkflowDAG.Edge(2L, 3L));

        PEWorkflowDAG peWorkflowDAG = new PEWorkflowDAG(nodes, edges);

        SaveWorkflowDAGRequest saveWorkflowDAGRequest = new SaveWorkflowDAGRequest();
        saveWorkflowDAGRequest.setId(WF_ID);
        saveWorkflowDAGRequest.setDag(peWorkflowDAG);
        ResultDTO<Void> res = ohMyClient.saveWorkflowDag(saveWorkflowDAGRequest);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }


    @Test
    void testDisableWorkflow() {
        ResultDTO<Void> res = ohMyClient.disableWorkflow(WF_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testDeleteWorkflow() {
        ResultDTO<Void> res = ohMyClient.deleteWorkflow(WF_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testEnableWorkflow() {
        ResultDTO<Void> res = ohMyClient.enableWorkflow(WF_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testFetchWorkflowInfo() {
        ResultDTO<WorkflowInfoDTO> res = ohMyClient.fetchWorkflow(WF_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testRunWorkflow() {
        ResultDTO<Long> res = ohMyClient.runWorkflow(WF_ID);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testStopWorkflowInstance() {
        ResultDTO<Void> res = ohMyClient.stopWorkflowInstance(149962433421639744L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testRetryWorkflowInstance() {
        ResultDTO<Void> res = ohMyClient.retryWorkflowInstance(149962433421639744L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testMarkWorkflowNodeAsSuccess() {
        ResultDTO<Void> res = ohMyClient.markWorkflowNodeAsSuccess(149962433421639744L, 1L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testFetchWfInstanceInfo() {
        ResultDTO<WorkflowInstanceInfoDTO> res = ohMyClient.fetchWorkflowInstanceInfo(149962433421639744L);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

    @Test
    void testRunWorkflowPlus() {
        ResultDTO<Long> res = ohMyClient.runWorkflow(WF_ID, "this is init Params 2", 90000);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }
}
