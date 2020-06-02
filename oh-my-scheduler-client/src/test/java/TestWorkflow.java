import com.github.kfcfans.oms.client.OhMyClient;
import com.github.kfcfans.oms.common.TimeExpressionType;
import com.github.kfcfans.oms.common.model.PEWorkflowDAG;
import com.github.kfcfans.oms.common.request.http.SaveWorkflowRequest;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 测试 Client（workflow部分）
 *
 * @author tjq
 * @since 2020/6/2
 */
public class TestWorkflow {

    private static OhMyClient ohMyClient;

    @BeforeAll
    public static void initClient() throws Exception {
        ohMyClient = new OhMyClient("127.0.0.1:7700", "oms-test");
    }

    @Test
    public void testSaveWorkflow() throws Exception {

        // DAG 图
        List<PEWorkflowDAG.Node> nodes = Lists.newLinkedList();

        nodes.add(new PEWorkflowDAG.Node(1L, "node-1", null, false, null));
        nodes.add(new PEWorkflowDAG.Node(2L, "node-2", null, false, null));

        PEWorkflowDAG peWorkflowDAG = new PEWorkflowDAG(nodes, null);
        SaveWorkflowRequest req = new SaveWorkflowRequest();

        req.setWfName("workflow-by-client");
        req.setWfDescription("created by client");
        req.setPEWorkflowDAG(peWorkflowDAG);
        req.setEnable(true);
        req.setTimeExpressionType(TimeExpressionType.API);

        System.out.println(ohMyClient.saveWorkflow(req));
    }

    @Test
    public void testDisableWorkflow() throws Exception {
        System.out.println(ohMyClient.disableWorkflow(1L));
    }

    @Test
    public void testDeleteWorkflow() throws Exception {
        System.out.println(ohMyClient.deleteWorkflow(1L));
    }

    @Test
    public void testEnableWorkflow() throws Exception {
        System.out.println(ohMyClient.enableWorkflow(1L));
    }

    @Test
    public void testFetchWorkflowInfo() throws Exception {
        System.out.println(ohMyClient.fetchWorkflow(1L));
    }

    @Test
    public void testRunWorkflow() throws Exception {
        System.out.println(ohMyClient.runWorkflow(1L));
    }

    @Test
    public void testStopWorkflowInstance() throws Exception {
        System.out.println(ohMyClient.stopWorkflowInstance(148003202598436928L));
    }

    @Test
    public void testFetchWfInstanceInfo() throws Exception {
        System.out.println(ohMyClient.fetchWorkflowInstanceInfo(148003202598436928L));
    }
}
