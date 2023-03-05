package tech.powerjob.common;

/**
 * OpenAPI 常量
 *
 * @author tjq
 * @since 2020/4/15
 */
public class OpenAPIConstant {

    private OpenAPIConstant(){

    }

    public static final String WEB_PATH = "/openApi";

    public static final String ASSERT = "/assert";

    /* ************* JOB 区 ************* */

    public static final String SAVE_JOB = "/saveJob";
    public static final String COPY_JOB = "/copyJob";

    public static final String EXPORT_JOB = "/exportJob";
    public static final String FETCH_JOB = "/fetchJob";
    public static final String FETCH_ALL_JOB = "/fetchAllJob";
    public static final String QUERY_JOB = "/queryJob";
    public static final String DISABLE_JOB = "/disableJob";
    public static final String ENABLE_JOB = "/enableJob";
    public static final String DELETE_JOB = "/deleteJob";
    public static final String RUN_JOB = "/runJob";

    /* ************* Instance 区 ************* */

    public static final String STOP_INSTANCE = "/stopInstance";
    public static final String CANCEL_INSTANCE = "/cancelInstance";
    public static final String RETRY_INSTANCE = "/retryInstance";
    public static final String FETCH_INSTANCE_STATUS = "/fetchInstanceStatus";
    public static final String FETCH_INSTANCE_INFO = "/fetchInstanceInfo";
    public static final String QUERY_INSTANCE = "/queryInstance";

    /* ************* Workflow 区 ************* */

    public static final String SAVE_WORKFLOW = "/saveWorkflow";
    public static final String COPY_WORKFLOW = "/copyWorkflow";
    public static final String FETCH_WORKFLOW = "/fetchWorkflow";
    public static final String DISABLE_WORKFLOW = "/disableWorkflow";
    public static final String ENABLE_WORKFLOW = "/enableWorkflow";
    public static final String DELETE_WORKFLOW = "/deleteWorkflow";
    public static final String RUN_WORKFLOW = "/runWorkflow";
    public static final String SAVE_WORKFLOW_NODE = "/addWorkflowNode";

    /* ************* WorkflowInstance 区 ************* */

    public static final String STOP_WORKFLOW_INSTANCE = "/stopWfInstance";
    public static final String RETRY_WORKFLOW_INSTANCE = "/retryWfInstance";
    public static final String FETCH_WORKFLOW_INSTANCE_INFO = "/fetchWfInstanceInfo";
    public static final String MARK_WORKFLOW_NODE_AS_SUCCESS = "/markWorkflowNodeAsSuccess";
}
