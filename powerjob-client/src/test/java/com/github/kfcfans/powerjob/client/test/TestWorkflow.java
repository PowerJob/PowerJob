package com.github.kfcfans.powerjob.client.test;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.client.OhMyClient;
import com.github.kfcfans.powerjob.common.ExecuteType;
import com.github.kfcfans.powerjob.common.ProcessorType;
import com.github.kfcfans.powerjob.common.TimeExpressionType;
import com.github.kfcfans.powerjob.common.model.PEWorkflowDAG;
import com.github.kfcfans.powerjob.common.request.http.*;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Test cases for {@link OhMyClient} workflow.
 *
 * @author tjq
 * @since 2020/6/2
 */
class TestWorkflow extends ClientInitializer {

    private static final long WF_ID = 1;

    @Test
    public void initTestData() throws Exception {
        SaveJobInfoRequest base = new SaveJobInfoRequest();
        base.setJobName("DAG-Node-");
        base.setTimeExpressionType(TimeExpressionType.WORKFLOW);
        base.setExecuteType(ExecuteType.STANDALONE);
        base.setProcessorType(ProcessorType.EMBEDDED_JAVA);
        base.setProcessorInfo("com.github.kfcfans.powerjob.samples.workflow.WorkflowStandaloneProcessor");

        for (int i = 0; i < 5; i++) {
            SaveJobInfoRequest request = JSONObject.parseObject(JSONObject.toJSONBytes(base), SaveJobInfoRequest.class);
            request.setJobName(request.getJobName() + i);
            System.out.println(ohMyClient.saveJob(request));
        }
    }

    @Test
    public void testSaveWorkflow() throws Exception {


        SaveWorkflowRequest req = new SaveWorkflowRequest();

        req.setWfName("workflow-by-client");
        req.setWfDescription("created by client");
        req.setEnable(true);
        req.setTimeExpressionType(TimeExpressionType.API);

        System.out.println("req ->" + JSONObject.toJSON(req));
        System.out.println(ohMyClient.saveWorkflow(req));

    }

    @Test
    public void testAddWorkflowNode(){
        AddWorkflowNodeRequest addWorkflowNodeRequest = new AddWorkflowNodeRequest();
        addWorkflowNodeRequest.setJobId(1L);
        addWorkflowNodeRequest.setWorkflowId(WF_ID);
        System.out.println(ohMyClient.addWorkflowNode(Lists.newArrayList(addWorkflowNodeRequest)));
    }

    @Test
    public void testModifyWorkflowNode() {
        ModifyWorkflowNodeRequest modifyWorkflowNodeRequest = new ModifyWorkflowNodeRequest();
        modifyWorkflowNodeRequest.setWorkflowId(WF_ID);
        modifyWorkflowNodeRequest.setId(1L);
        modifyWorkflowNodeRequest.setNodeAlias("(๑•̀ㅂ•́)و✧");
        modifyWorkflowNodeRequest.setEnable(false);
        modifyWorkflowNodeRequest.setSkipWhenFailed(false);
        System.out.println(ohMyClient.modifyWorkflowNode(modifyWorkflowNodeRequest));
    }

    @Test
    public void testSaveWorkflowDag() {
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

        System.out.println(ohMyClient.saveWorkflowDag(saveWorkflowDAGRequest));
    }




    @Test
    public void testDisableWorkflow() throws Exception {
        System.out.println(ohMyClient.disableWorkflow(WF_ID));
    }

    @Test
    public void testDeleteWorkflow() throws Exception {
        System.out.println(ohMyClient.deleteWorkflow(WF_ID));
    }

    @Test
    public void testEnableWorkflow() throws Exception {
        System.out.println(ohMyClient.enableWorkflow(WF_ID));
    }

    @Test
    public void testFetchWorkflowInfo() throws Exception {
        System.out.println(ohMyClient.fetchWorkflow(WF_ID));
    }

    @Test
    public void testRunWorkflow() throws Exception {
        System.out.println(ohMyClient.runWorkflow(WF_ID));
    }

    @Test
    public void testStopWorkflowInstance() throws Exception {
        System.out.println(ohMyClient.stopWorkflowInstance(149962433421639744L));
    }

    @Test
    public void testRetryWorkflowInstance() {
        System.out.println(ohMyClient.retryWorkflowInstance(149962433421639744L));
    }

    @Test
    public void testFetchWfInstanceInfo() throws Exception {
        System.out.println(ohMyClient.fetchWorkflowInstanceInfo(149962433421639744L));
    }

    @Test
    public void testRunWorkflowPlus() throws Exception {
        System.out.println(ohMyClient.runWorkflow(WF_ID, "this is init Params 2", 90000));
    }
}
