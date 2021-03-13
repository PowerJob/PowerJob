package com.github.kfcfans.powerjob.client.test;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.client.OhMyClient;
import com.github.kfcfans.powerjob.common.enums.ExecuteType;
import com.github.kfcfans.powerjob.common.enums.ProcessorType;
import com.github.kfcfans.powerjob.common.enums.TimeExpressionType;
import com.github.kfcfans.powerjob.common.enums.WorkflowNodeType;
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
        base.setProcessorType(ProcessorType.BUILT_IN);
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

        req.setId(res.getData());

        // 创建节点
        SaveWorkflowNodeRequest saveWorkflowNodeRequest1 = new SaveWorkflowNodeRequest();
        saveWorkflowNodeRequest1.setJobId(1L);
        saveWorkflowNodeRequest1.setWorkflowId(req.getId());
        saveWorkflowNodeRequest1.setNodeName("DAG-Node-1");
        saveWorkflowNodeRequest1.setType(WorkflowNodeType.JOB);

        SaveWorkflowNodeRequest saveWorkflowNodeRequest2 = new SaveWorkflowNodeRequest();
        saveWorkflowNodeRequest2.setJobId(1L);
        saveWorkflowNodeRequest2.setWorkflowId(req.getId());
        saveWorkflowNodeRequest2.setNodeName("DAG-Node-2");
        saveWorkflowNodeRequest2.setType(WorkflowNodeType.JOB);


        SaveWorkflowNodeRequest saveWorkflowNodeRequest3 = new SaveWorkflowNodeRequest();
        saveWorkflowNodeRequest3.setJobId(1L);
        saveWorkflowNodeRequest3.setWorkflowId(req.getId());
        saveWorkflowNodeRequest3.setNodeName("DAG-Node-3");
        saveWorkflowNodeRequest3.setType(WorkflowNodeType.JOB);


        List<WorkflowNodeInfoDTO> nodeList = ohMyClient.saveWorkflowNode(Lists.newArrayList(saveWorkflowNodeRequest1,saveWorkflowNodeRequest2,saveWorkflowNodeRequest3)).getData();
        System.out.println(nodeList);
        Assertions.assertNotNull(nodeList);


        // DAG 图
        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        nodes.add(new PEWorkflowDAG.Node(nodeList.get(0).getId(), 1L, "DAG-Node-1"));
        nodes.add(new PEWorkflowDAG.Node(nodeList.get(1).getId(), 1L, "DAG-Node-2"));
        nodes.add(new PEWorkflowDAG.Node(nodeList.get(2).getId(), 1L, "DAG-Node-3"));

        edges.add(new PEWorkflowDAG.Edge(nodeList.get(0).getId(), nodeList.get(1).getId()));
        edges.add(new PEWorkflowDAG.Edge(nodeList.get(1).getId(), nodeList.get(2).getId()));
        PEWorkflowDAG peWorkflowDAG = new PEWorkflowDAG(nodes, edges);

        // 保存完整信息
        req.setDag(peWorkflowDAG);
        res = ohMyClient.saveWorkflow(req);

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
