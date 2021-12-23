package tech.powerjob.server.core.workflow.hanlder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.server.core.workflow.hanlder.impl.DecisionNodeHandler;
import tech.powerjob.server.persistence.remote.model.WorkflowInstanceInfoDO;


import static tech.powerjob.server.core.data.DataConstructUtil.*;

/**
 * @author Echo009
 * @since 2021/12/9
 * <p>
 * 如有变动，请同步变更文档
 * https://www.yuque.com/powerjob/dev/bgw03h/edit?toc_node_uuid=V9igz9SZ30lF59bX
 */
class DecisionNodeHandlerTest {

    private final DecisionNodeHandler decisionNodeHandler = new DecisionNodeHandler();


    @Test
    void testCase1() {

        PEWorkflowDAG peWorkflowDAG = constructEmptyDAG();
        PEWorkflowDAG.Node node1 = new PEWorkflowDAG.Node(1L, WorkflowNodeType.DECISION.getCode());
        // decision node return true
        node1.setNodeParams("true;");
        PEWorkflowDAG.Node node2 = new PEWorkflowDAG.Node(2L);
        PEWorkflowDAG.Node node3 = new PEWorkflowDAG.Node(3L);
        PEWorkflowDAG.Node node4 = new PEWorkflowDAG.Node(4L);
        addNodes(peWorkflowDAG, node1, node2, node3, node4);

        PEWorkflowDAG.Edge edge1_2 = new PEWorkflowDAG.Edge(1L, 2L, "false");
        PEWorkflowDAG.Edge edge1_3 = new PEWorkflowDAG.Edge(1L, 3L, "true");
        PEWorkflowDAG.Edge edge2_4 = new PEWorkflowDAG.Edge(2L, 4L);
        addEdges(peWorkflowDAG, edge1_2, edge1_3, edge2_4);

        decisionNodeHandler.handle(node1, peWorkflowDAG, new WorkflowInstanceInfoDO());
        Assertions.assertEquals(false, node2.getEnable());
        Assertions.assertEquals(true, node2.getDisableByControlNode());
        Assertions.assertEquals(false, node4.getEnable());
        Assertions.assertEquals(true, node4.getDisableByControlNode());
        //
        Assertions.assertEquals(false, edge1_2.getEnable());
        Assertions.assertEquals(false, edge2_4.getEnable());

    }


    @Test
    void testCase2() {

        PEWorkflowDAG peWorkflowDAG = constructEmptyDAG();
        PEWorkflowDAG.Node node1 = new PEWorkflowDAG.Node(1L, WorkflowNodeType.DECISION.getCode());
        // decision node return true
        node1.setNodeParams("true;");
        PEWorkflowDAG.Node node2 = new PEWorkflowDAG.Node(2L);
        PEWorkflowDAG.Node node3 = new PEWorkflowDAG.Node(3L);
        PEWorkflowDAG.Node node4 = new PEWorkflowDAG.Node(4L);
        PEWorkflowDAG.Node node5 = new PEWorkflowDAG.Node(5L);
        addNodes(peWorkflowDAG, node1, node2, node3, node4, node5);

        PEWorkflowDAG.Edge edge1_2 = new PEWorkflowDAG.Edge(1L, 2L, "false");
        PEWorkflowDAG.Edge edge1_3 = new PEWorkflowDAG.Edge(1L, 3L, "true");
        PEWorkflowDAG.Edge edge2_4 = new PEWorkflowDAG.Edge(2L, 4L);
        PEWorkflowDAG.Edge edge2_5 = new PEWorkflowDAG.Edge(2L, 5L);
        PEWorkflowDAG.Edge edge3_5 = new PEWorkflowDAG.Edge(3L, 5L);
        addEdges(peWorkflowDAG, edge1_2, edge1_3, edge2_4, edge2_5, edge3_5);

        decisionNodeHandler.handle(node1, peWorkflowDAG, new WorkflowInstanceInfoDO());
        Assertions.assertEquals(false, node2.getEnable());
        Assertions.assertEquals(true, node2.getDisableByControlNode());
        Assertions.assertEquals(false, node4.getEnable());
        Assertions.assertEquals(true, node4.getDisableByControlNode());
        //
        Assertions.assertEquals(false, edge1_2.getEnable());
        Assertions.assertEquals(false, edge2_4.getEnable());
        Assertions.assertEquals(false, edge2_5.getEnable());

    }

    @Test
    void testCase3() {

        PEWorkflowDAG peWorkflowDAG = constructEmptyDAG();
        PEWorkflowDAG.Node node1 = new PEWorkflowDAG.Node(1L, WorkflowNodeType.DECISION.getCode());
        // decision node return true
        node1.setNodeParams("true;");
        PEWorkflowDAG.Node node2 = new PEWorkflowDAG.Node(2L, WorkflowNodeType.DECISION.getCode());
        // decision node return true
        node2.setNodeParams("true;");
        PEWorkflowDAG.Node node3 = new PEWorkflowDAG.Node(3L);
        PEWorkflowDAG.Node node4 = new PEWorkflowDAG.Node(4L);
        PEWorkflowDAG.Node node5 = new PEWorkflowDAG.Node(5L);
        addNodes(peWorkflowDAG, node1, node2, node3, node4, node5);

        PEWorkflowDAG.Edge edge1_2 = new PEWorkflowDAG.Edge(1L, 2L, "true");
        PEWorkflowDAG.Edge edge1_3 = new PEWorkflowDAG.Edge(1L, 3L, "false");
        PEWorkflowDAG.Edge edge2_5 = new PEWorkflowDAG.Edge(2L, 5L, "false");
        PEWorkflowDAG.Edge edge2_4 = new PEWorkflowDAG.Edge(2L, 4L, "true");
        PEWorkflowDAG.Edge edge3_5 = new PEWorkflowDAG.Edge(3L, 5L);
        addEdges(peWorkflowDAG, edge1_2, edge1_3, edge2_4, edge2_5, edge3_5);
        // 处理第一个判断节点后
        decisionNodeHandler.handle(node1, peWorkflowDAG, new WorkflowInstanceInfoDO());
        Assertions.assertEquals(false, node3.getEnable());
        Assertions.assertEquals(true, node3.getDisableByControlNode());
        //
        Assertions.assertEquals(false, edge1_3.getEnable());
        Assertions.assertEquals(false, edge3_5.getEnable());
        Assertions.assertNull(edge2_5.getEnable());
        // 节点 5 还是初始状态
        Assertions.assertNull(node5.getEnable());
        // 处理第二个判断节点
        decisionNodeHandler.handle(node2, peWorkflowDAG, new WorkflowInstanceInfoDO());
        // 节点 5 被 disable
        Assertions.assertFalse(node5.getEnable());
        Assertions.assertFalse(edge2_5.getEnable());
    }


    @Test
    void testCase4() {

        PEWorkflowDAG peWorkflowDAG = constructEmptyDAG();
        PEWorkflowDAG.Node node1 = new PEWorkflowDAG.Node(1L, WorkflowNodeType.DECISION.getCode());
        // decision node return true
        node1.setNodeParams("true;");
        PEWorkflowDAG.Node node2 = new PEWorkflowDAG.Node(2L, WorkflowNodeType.DECISION.getCode());
        // decision node return true
        node2.setNodeParams("true;");
        PEWorkflowDAG.Node node3 = new PEWorkflowDAG.Node(3L);
        PEWorkflowDAG.Node node4 = new PEWorkflowDAG.Node(4L);
        PEWorkflowDAG.Node node5 = new PEWorkflowDAG.Node(5L);
        addNodes(peWorkflowDAG, node1, node2, node3, node4, node5);

        PEWorkflowDAG.Edge edge1_2 = new PEWorkflowDAG.Edge(1L, 2L, "true");
        PEWorkflowDAG.Edge edge1_3 = new PEWorkflowDAG.Edge(1L, 3L, "false");
        PEWorkflowDAG.Edge edge2_5 = new PEWorkflowDAG.Edge(2L, 5L, "true");
        PEWorkflowDAG.Edge edge2_4 = new PEWorkflowDAG.Edge(2L, 4L, "false");
        PEWorkflowDAG.Edge edge3_5 = new PEWorkflowDAG.Edge(3L, 5L);
        addEdges(peWorkflowDAG, edge1_2, edge1_3, edge2_4, edge2_5, edge3_5);
        // 处理第一个判断节点后
        decisionNodeHandler.handle(node1, peWorkflowDAG, new WorkflowInstanceInfoDO());
        Assertions.assertEquals(false, node3.getEnable());
        Assertions.assertEquals(true, node3.getDisableByControlNode());
        //
        Assertions.assertEquals(false, edge1_3.getEnable());
        Assertions.assertEquals(false, edge3_5.getEnable());
        Assertions.assertNull(edge2_5.getEnable());
        // 节点 5 还是初始状态
        Assertions.assertNull(node5.getEnable());
        // 处理第二个判断节点
        decisionNodeHandler.handle(node2, peWorkflowDAG, new WorkflowInstanceInfoDO());
        // 节点 5 还是初始状态
        Assertions.assertNull(node5.getEnable());
        Assertions.assertFalse(node4.getEnable());
        Assertions.assertTrue(node4.getDisableByControlNode());
        Assertions.assertFalse(edge2_4.getEnable());
    }

}
