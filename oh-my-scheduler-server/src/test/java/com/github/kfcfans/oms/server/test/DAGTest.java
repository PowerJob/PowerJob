package com.github.kfcfans.oms.server.test;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.oms.common.model.PEWorkflowDAG;
import com.github.kfcfans.oms.server.model.WorkflowDAG;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.server.common.utils.WorkflowDAGUtils;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

/**
 * DAG 图算法测试集合
 *
 * @author tjq
 * @since 2020/5/31
 */
public class DAGTest {

    @Test
    public void testDAGUtils() throws Exception {

        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        // 图1： 1 -> 2 -> 1，理论上报错
        nodes.add(new PEWorkflowDAG.Node(1L, "1", null, false, null));
        nodes.add(new PEWorkflowDAG.Node(2L, "2", null, false, null));
        edges.add(new PEWorkflowDAG.Edge(1L, 2L));
        edges.add(new PEWorkflowDAG.Edge(2L, 1L));
        System.out.println(WorkflowDAGUtils.valid(new PEWorkflowDAG(nodes, edges)));

        // 图2： 1 -> 2/3 -> 4
        List<PEWorkflowDAG.Node> nodes2 = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges2 = Lists.newLinkedList();

        nodes2.add(new PEWorkflowDAG.Node(1L, "1", null, false, null));
        nodes2.add(new PEWorkflowDAG.Node(2L, "2", null, false, null));
        nodes2.add(new PEWorkflowDAG.Node(3L, "3", null, false, null));
        nodes2.add(new PEWorkflowDAG.Node(4L, "4", null, false, null));
        edges2.add(new PEWorkflowDAG.Edge(1L, 2L));
        edges2.add(new PEWorkflowDAG.Edge(1L, 3L));
        edges2.add(new PEWorkflowDAG.Edge(2L, 4L));
        edges2.add(new PEWorkflowDAG.Edge(3L, 4L));

        PEWorkflowDAG validPEDAG = new PEWorkflowDAG(nodes2, edges2);
        System.out.println(WorkflowDAGUtils.valid(validPEDAG));

        WorkflowDAG wfDAG = WorkflowDAGUtils.convert(validPEDAG);
        System.out.println("jackson");
        System.out.println(JsonUtils.toJSONString(wfDAG));

        // Jackson 不知道怎么序列化引用，只能放弃，使用 FastJSON 序列化引用，即 $ref
        WorkflowDAG wfDAGByJackSon = JsonUtils.parseObject(JsonUtils.toJSONString(wfDAG), WorkflowDAG.class);

        System.out.println("fastJson");
        System.out.println(JSONObject.toJSONString(wfDAG));
        WorkflowDAG wfDAGByFastJSON = JSONObject.parseObject(JSONObject.toJSONString(wfDAG), WorkflowDAG.class);

        // 打断点看 reference 关系
        System.out.println(wfDAGByJackSon);
        System.out.println(wfDAGByFastJSON);

        // 测试图三（双顶点） 1 -> 3, 2 -> 4
        List<PEWorkflowDAG.Node> nodes3 = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges3 = Lists.newLinkedList();

        nodes3.add(new PEWorkflowDAG.Node(1L, "1", null, false, null));
        nodes3.add(new PEWorkflowDAG.Node(2L, "2", null, false, null));
        nodes3.add(new PEWorkflowDAG.Node(3L, "3", null, false, null));
        nodes3.add(new PEWorkflowDAG.Node(4L, "4", null, false, null));
        edges3.add(new PEWorkflowDAG.Edge(1L, 3L));
        edges3.add(new PEWorkflowDAG.Edge(2L, 4L));

        PEWorkflowDAG multiRootPEDAG = new PEWorkflowDAG(nodes3, edges3);
        System.out.println(WorkflowDAGUtils.valid(multiRootPEDAG));
        WorkflowDAG multiRootDAG = WorkflowDAGUtils.convert(multiRootPEDAG);
        System.out.println(multiRootDAG);

    }


}
