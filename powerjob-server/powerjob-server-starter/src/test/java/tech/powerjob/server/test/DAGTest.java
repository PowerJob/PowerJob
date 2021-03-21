package tech.powerjob.server.test;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAGUtils;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAG;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DAG 图算法测试集合
 *
 * @author tjq
 * @author Echo009
 * @since 2020/5/31
 */
public class DAGTest {


    @Test
    public void testValidDAG1() {
        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        // 测试图1： 1 -> 2 -> 1，理论上报错
        nodes.add(new PEWorkflowDAG.Node(1L));
        nodes.add(new PEWorkflowDAG.Node(2L));
        edges.add(new PEWorkflowDAG.Edge(1L, 2L));
        edges.add(new PEWorkflowDAG.Edge(2L, 1L));
        Assert.assertFalse(WorkflowDAGUtils.valid(new PEWorkflowDAG(nodes, edges)));
    }

    @Test
    public void testValidDAG2() throws JsonProcessingException {
        // 测试图2： 1 -> 2/3 -> 4
        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        nodes.add(new PEWorkflowDAG.Node(1L));
        nodes.add(new PEWorkflowDAG.Node(2L));
        nodes.add(new PEWorkflowDAG.Node(3L));
        nodes.add(new PEWorkflowDAG.Node(4L));
        edges.add(new PEWorkflowDAG.Edge(1L, 2L));
        edges.add(new PEWorkflowDAG.Edge(1L, 3L));
        edges.add(new PEWorkflowDAG.Edge(2L, 4L));
        edges.add(new PEWorkflowDAG.Edge(3L, 4L));

        PEWorkflowDAG validPEDAG = new PEWorkflowDAG(nodes, edges);
        Assert.assertTrue(WorkflowDAGUtils.valid(validPEDAG));

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
    }

    @Test
    public void testValidDAG3() {

        // 测试图3：（双顶点） 1 -> 3, 2 -> 4
        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        nodes.add(new PEWorkflowDAG.Node(1L));
        nodes.add(new PEWorkflowDAG.Node(2L));
        nodes.add(new PEWorkflowDAG.Node(3L));
        nodes.add(new PEWorkflowDAG.Node(4L));
        edges.add(new PEWorkflowDAG.Edge(1L, 3L));
        edges.add(new PEWorkflowDAG.Edge(2L, 4L));

        PEWorkflowDAG multiRootPEDAG = new PEWorkflowDAG(nodes, edges);
        Assert.assertTrue(WorkflowDAGUtils.valid(multiRootPEDAG));
        WorkflowDAG multiRootDAG = WorkflowDAGUtils.convert(multiRootPEDAG);
        System.out.println(multiRootDAG);
    }

    /**
     * @author Echo009
     * @since 2021/02/21
     */
    @Test
    public void testValidDAG4() {

        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        // 测试图4：（双顶点 单个环） 1 -> 3 -> 1, 2 -> 4
        nodes.add(new PEWorkflowDAG.Node(1L));
        nodes.add(new PEWorkflowDAG.Node(2L));
        nodes.add(new PEWorkflowDAG.Node(3L));
        nodes.add(new PEWorkflowDAG.Node(4L));
        edges.add(new PEWorkflowDAG.Edge(1L, 3L));
        edges.add(new PEWorkflowDAG.Edge(3L, 1L));
        edges.add(new PEWorkflowDAG.Edge(2L, 4L));

        Assert.assertFalse(WorkflowDAGUtils.valid(new PEWorkflowDAG(nodes, edges)));

    }


    /**
     * @author Echo009
     * @since 2021/02/21
     */
    @Test
    public void testValidDAG5() {

        //  1 -> 2 -> 5 -> 6
        //       3 -> 5
        //  1 -> 3 -> 4 -> 5
        //  1 -> 6

        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        nodes.add(new PEWorkflowDAG.Node(1L));
        nodes.add(new PEWorkflowDAG.Node(2L));
        nodes.add(new PEWorkflowDAG.Node(3L));
        nodes.add(new PEWorkflowDAG.Node(4L));
        nodes.add(new PEWorkflowDAG.Node(5L));
        nodes.add(new PEWorkflowDAG.Node(6L));
        edges.add(new PEWorkflowDAG.Edge(1L, 2L));
        edges.add(new PEWorkflowDAG.Edge(2L, 5L));
        edges.add(new PEWorkflowDAG.Edge(5L, 6L));
        edges.add(new PEWorkflowDAG.Edge(1L, 3L));
        edges.add(new PEWorkflowDAG.Edge(3L, 4L));
        edges.add(new PEWorkflowDAG.Edge(3L, 5L));
        edges.add(new PEWorkflowDAG.Edge(4L, 5L));
        edges.add(new PEWorkflowDAG.Edge(1L, 6L));


        Assert.assertTrue(WorkflowDAGUtils.valid(new PEWorkflowDAG(nodes, edges)));

    }



    /**
     * @author Echo009
     * @since 2021/02/07
     */
    @Test
    public void testListReadyNodes1() {
        // 双顶点
        // 1 -> 3
        // 2(x) -> 4 -> 5
        // 6(x) -> 7(x) -> 8(x) -> 4
        //                 8(x) -> 9

        List<PEWorkflowDAG.Node> nodes1 = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges1 = Lists.newLinkedList();

        nodes1.add(new PEWorkflowDAG.Node(1L));
        nodes1.add(new PEWorkflowDAG.Node(2L).setEnable(false));
        nodes1.add(new PEWorkflowDAG.Node(3L));
        nodes1.add(new PEWorkflowDAG.Node(4L));
        nodes1.add(new PEWorkflowDAG.Node(5L));
        nodes1.add(new PEWorkflowDAG.Node(6L).setEnable(false));
        nodes1.add(new PEWorkflowDAG.Node(7L).setEnable(false));
        nodes1.add(new PEWorkflowDAG.Node(8L).setEnable(false));
        nodes1.add(new PEWorkflowDAG.Node(9L));
        edges1.add(new PEWorkflowDAG.Edge(1L, 3L));
        edges1.add(new PEWorkflowDAG.Edge(2L, 4L));
        edges1.add(new PEWorkflowDAG.Edge(4L, 5L));
        edges1.add(new PEWorkflowDAG.Edge(4L, 5L));
        edges1.add(new PEWorkflowDAG.Edge(6L, 7L));
        edges1.add(new PEWorkflowDAG.Edge(7L, 8L));
        edges1.add(new PEWorkflowDAG.Edge(8L, 4L));
        edges1.add(new PEWorkflowDAG.Edge(8L, 9L));

        PEWorkflowDAG dag1 = new PEWorkflowDAG(nodes1, edges1);
        List<Long> readyNodeIds1 = WorkflowDAGUtils.listReadyNodes(dag1).stream().map(PEWorkflowDAG.Node::getNodeId).collect(Collectors.toList());

        System.out.println(readyNodeIds1);
        Assert.assertTrue(readyNodeIds1.contains(1L));
        Assert.assertTrue(readyNodeIds1.contains(4L));
        Assert.assertTrue(readyNodeIds1.contains(9L));

    }

    /**
     * @author Echo009
     * @since 2021/02/07
     */
    @Test
    public void testListReadyNodes2() {

        //  注：(x) 代表 enable = false 的节点
        // 测试连续 move
        //  1(x) -> 2(x) -> 3 -> 4 -> 5(x) -> 6

        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        nodes.add(new PEWorkflowDAG.Node(1L).setEnable(false));
        nodes.add(new PEWorkflowDAG.Node(2L).setEnable(false));
        nodes.add(new PEWorkflowDAG.Node(3L));
        nodes.add(new PEWorkflowDAG.Node(4L));
        nodes.add(new PEWorkflowDAG.Node(5L).setEnable(false));
        nodes.add(new PEWorkflowDAG.Node(6L));
        edges.add(new PEWorkflowDAG.Edge(1L, 2L));
        edges.add(new PEWorkflowDAG.Edge(2L, 3L));
        edges.add(new PEWorkflowDAG.Edge(3L, 4L));
        edges.add(new PEWorkflowDAG.Edge(4L, 5L));
        edges.add(new PEWorkflowDAG.Edge(5L, 6L));

        PEWorkflowDAG dag = new PEWorkflowDAG(nodes, edges);
        List<Long> readyNodeIds2 = WorkflowDAGUtils.listReadyNodes(dag).stream().map(PEWorkflowDAG.Node::getNodeId).collect(Collectors.toList());

        System.out.println(readyNodeIds2);

        Assert.assertEquals(1, readyNodeIds2.size());
        Assert.assertTrue(readyNodeIds2.contains(3L));

    }


    /**
     * @author Echo009
     * @since 2021/02/07
     */
    @Test
    public void testListReadyNodes3() {

        //  注：(x) 代表 enable = false 的节点
        //  复杂 move
        //  1(failed) -> 2(x) -> 4 -> 5(x) -> 6
        //  3(success) -> 4
        //  7 -> 6

        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        nodes.add(new PEWorkflowDAG.Node(1L).setStatus(InstanceStatus.FAILED.getV()));
        nodes.add(new PEWorkflowDAG.Node(2L).setEnable(false));
        nodes.add(new PEWorkflowDAG.Node(3L).setStatus(InstanceStatus.SUCCEED.getV()));
        nodes.add(new PEWorkflowDAG.Node(4L));
        nodes.add(new PEWorkflowDAG.Node(5L).setEnable(false));
        nodes.add(new PEWorkflowDAG.Node(6L));
        nodes.add(new PEWorkflowDAG.Node(7L));
        edges.add(new PEWorkflowDAG.Edge(1L, 2L));
        edges.add(new PEWorkflowDAG.Edge(2L, 4L));
        edges.add(new PEWorkflowDAG.Edge(3L, 4L));
        edges.add(new PEWorkflowDAG.Edge(4L, 5L));
        edges.add(new PEWorkflowDAG.Edge(5L, 6L));
        edges.add(new PEWorkflowDAG.Edge(7L, 6L));

        PEWorkflowDAG dag = new PEWorkflowDAG(nodes, edges);
        List<Long> readyNodeIds2 = WorkflowDAGUtils.listReadyNodes(dag).stream().map(PEWorkflowDAG.Node::getNodeId).collect(Collectors.toList());

        System.out.println(readyNodeIds2);

        Assert.assertEquals(2, readyNodeIds2.size());
        Assert.assertTrue(readyNodeIds2.contains(4L));
        Assert.assertTrue(readyNodeIds2.contains(7L));

    }


    /**
     * @author Echo009
     * @since 2021/02/07
     */
    @Test
    public void testListReadyNodes4() {

        //  注：(x) 代表 enable = false 的节点
        //  复杂 move
        //  1(failed) -> 2(x) -> 5 -> 6
        //               3(x) -> 5
        //  1(failed) -> 3(x) -> 4(x) -> 5
        //                       4(x) -> 6

        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        nodes.add(new PEWorkflowDAG.Node(1L).setStatus(InstanceStatus.FAILED.getV()));
        nodes.add(new PEWorkflowDAG.Node(2L).setEnable(false));
        nodes.add(new PEWorkflowDAG.Node(3L).setEnable(false));
        nodes.add(new PEWorkflowDAG.Node(4L).setEnable(false));
        nodes.add(new PEWorkflowDAG.Node(5L));
        nodes.add(new PEWorkflowDAG.Node(6L));
        edges.add(new PEWorkflowDAG.Edge(1L, 2L));
        edges.add(new PEWorkflowDAG.Edge(2L, 5L));
        edges.add(new PEWorkflowDAG.Edge(5L, 6L));
        edges.add(new PEWorkflowDAG.Edge(1L, 3L));
        edges.add(new PEWorkflowDAG.Edge(3L, 4L));
        edges.add(new PEWorkflowDAG.Edge(3L, 5L));
        edges.add(new PEWorkflowDAG.Edge(4L, 5L));

        PEWorkflowDAG dag = new PEWorkflowDAG(nodes, edges);
        List<Long> readyNodeIds2 = WorkflowDAGUtils.listReadyNodes(dag).stream().map(PEWorkflowDAG.Node::getNodeId).collect(Collectors.toList());

        System.out.println(readyNodeIds2);

        Assert.assertEquals(1, readyNodeIds2.size());
        Assert.assertTrue(readyNodeIds2.contains(5L));

    }


    /**
     * @author Echo009
     * @since 2021/02/07
     */
    @Test
    public void testListReadyNodes5() {

        //  注：(x) 代表 enable = false 的节点
        //  复杂 move
        //  1(failed) -> 2(x) -> 5 -> 6
        //               3(x) -> 5
        //  1(failed) -> 3(x) -> 4(x) -> 5
        //                       4(x) -> 6
        //                       4(x) -> 7

        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();

        nodes.add(new PEWorkflowDAG.Node(1L).setStatus(InstanceStatus.FAILED.getV()));
        nodes.add(new PEWorkflowDAG.Node(2L).setEnable(false));
        nodes.add(new PEWorkflowDAG.Node(3L).setEnable(false));
        nodes.add(new PEWorkflowDAG.Node(4L).setEnable(false));
        nodes.add(new PEWorkflowDAG.Node(5L));
        nodes.add(new PEWorkflowDAG.Node(6L));
        nodes.add(new PEWorkflowDAG.Node(7L));
        edges.add(new PEWorkflowDAG.Edge(1L, 2L));
        edges.add(new PEWorkflowDAG.Edge(2L, 5L));
        edges.add(new PEWorkflowDAG.Edge(5L, 6L));
        edges.add(new PEWorkflowDAG.Edge(1L, 3L));
        edges.add(new PEWorkflowDAG.Edge(3L, 4L));
        edges.add(new PEWorkflowDAG.Edge(3L, 5L));
        edges.add(new PEWorkflowDAG.Edge(4L, 5L));
        edges.add(new PEWorkflowDAG.Edge(4L, 7L));

        PEWorkflowDAG dag = new PEWorkflowDAG(nodes, edges);
        List<Long> readyNodeIds2 = WorkflowDAGUtils.listReadyNodes(dag).stream().map(PEWorkflowDAG.Node::getNodeId).collect(Collectors.toList());

        System.out.println(readyNodeIds2);

        Assert.assertEquals(2, readyNodeIds2.size());
        Assert.assertTrue(readyNodeIds2.contains(5L));
        Assert.assertTrue(readyNodeIds2.contains(7L));

    }

}
