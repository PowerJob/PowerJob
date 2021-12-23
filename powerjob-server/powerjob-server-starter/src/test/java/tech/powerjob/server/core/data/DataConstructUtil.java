package tech.powerjob.server.core.data;

import com.google.common.collect.Lists;
import tech.powerjob.common.model.PEWorkflowDAG;

import java.util.List;

/**
 * @author Echo009
 * @since 2021/12/10
 */
public class DataConstructUtil {

    public static void addNodes(PEWorkflowDAG dag, PEWorkflowDAG.Node... nodes) {
        for (PEWorkflowDAG.Node node : nodes) {
            dag.getNodes().add(node);
        }
    }

    public static void addEdges(PEWorkflowDAG dag, PEWorkflowDAG.Edge... edges) {
        for (PEWorkflowDAG.Edge edge : edges) {
            dag.getEdges().add(edge);
        }
    }

    public static PEWorkflowDAG constructEmptyDAG() {
        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();
        List<PEWorkflowDAG.Edge> edges = Lists.newLinkedList();
        return new PEWorkflowDAG(nodes, edges);
    }

}
