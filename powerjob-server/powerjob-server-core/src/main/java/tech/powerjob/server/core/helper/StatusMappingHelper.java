package tech.powerjob.server.core.helper;

import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.WorkflowInstanceStatus;

/**
 * @author Echo009
 * @since 2021/12/13
 */
public class StatusMappingHelper {

    private StatusMappingHelper(){

    }

    /**
     * 工作流实例状态转任务实例状态
     */
    public static InstanceStatus toInstanceStatus(WorkflowInstanceStatus workflowInstanceStatus) {
        switch (workflowInstanceStatus) {
            case FAILED:
                return InstanceStatus.FAILED;
            case SUCCEED:
                return InstanceStatus.SUCCEED;
            case RUNNING:
                return InstanceStatus.RUNNING;
            case STOPPED:
                return InstanceStatus.STOPPED;
            default:
                return null;
        }
    }


}
