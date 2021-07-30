package tech.powerjob.worker.core.processor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.serialize.JsonUtils;

import java.util.HashMap;


/**
 * @author Echo009
 * @since 2021/7/28
 */
class WorkflowContextTest {


    private static String workflowContextString;

    private static final String T_STRING = "this is a simple String";
    private static final Integer T_INTEGER = 9999;

    @BeforeAll
    public static void before() {

        WorkflowContext workflowContext = new WorkflowContext(1L, "");
        HashMap<String, Object> data = new HashMap<>();
        data.put("inner_key_1", "1");
        data.put("inner_key_2", 2);
        workflowContext.appendData2WfContext("key_1", data);
        workflowContext.appendData2WfContext("key_2", T_STRING);
        workflowContext.appendData2WfContext("key_3", T_INTEGER);
        workflowContextString = JsonUtils.toJSONString(workflowContext.getAppendedContextData());
    }


    @Test
    void test() {

        WorkflowContext context = new WorkflowContext(2L, workflowContextString);

        Assertions.assertEquals(T_STRING, context.fetchValueFromWorkflowContext("key_2",String.class));
        Assertions.assertEquals(T_INTEGER, context.fetchValueFromWorkflowContext("key_3",Integer.class));



    }


}