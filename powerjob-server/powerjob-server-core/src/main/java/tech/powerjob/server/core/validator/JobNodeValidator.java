package tech.powerjob.server.core.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.core.workflow.algorithm.WorkflowDAG;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.model.WorkflowNodeInfoDO;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;

import javax.annotation.Resource;

/**
 * @author Echo009
 * @since 2021/12/14
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JobNodeValidator implements NodeValidator {

    private final JobInfoRepository jobInfoRepository;

    @Override
    public void complexValidate(WorkflowNodeInfoDO node, WorkflowDAG dag) {
        // do nothing
    }

    @Override
    public void simpleValidate(WorkflowNodeInfoDO node) {
        // 判断对应的任务是否存在
        JobInfoDO job = jobInfoRepository.findById(node.getJobId())
                .orElseThrow(() -> new PowerJobException("Illegal job node,specified job is not exist,node name : " + node.getNodeName()));

        if (job.getStatus() == SwitchableStatus.DELETED.getV()) {
            throw new PowerJobException("Illegal job node,specified job has been deleted,node name : " + node.getNodeName());
        }
    }

    @Override
    public WorkflowNodeType matchingType() {
        return WorkflowNodeType.JOB;
    }
}
