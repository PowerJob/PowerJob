package com.github.kfcfans.oms.server.service.workflow;

import com.github.kfcfans.oms.common.OmsException;
import com.github.kfcfans.oms.common.TimeExpressionType;
import com.github.kfcfans.oms.common.request.http.SaveWorkflowRequest;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.common.utils.WorkflowDAGUtils;
import com.github.kfcfans.oms.server.common.SJ;
import com.github.kfcfans.oms.server.common.constans.SwitchableStatus;
import com.github.kfcfans.oms.server.common.utils.CronExpression;
import com.github.kfcfans.oms.server.persistence.core.model.WorkflowInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.WorkflowInfoRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * Workflow 服务
 *
 * @author tjq
 * @since 2020/5/26
 */
@Service
public class WorkflowService {

    @Resource
    private WorkflowInstanceManager workflowInstanceManager;
    @Resource
    private WorkflowInfoRepository workflowInfoRepository;

    /**
     * 保存/修改DAG工作流
     * @param req 请求
     * @return 工作流ID
     * @throws Exception 异常
     */
    public Long saveWorkflow(SaveWorkflowRequest req) throws Exception {

        if (!WorkflowDAGUtils.valid(req.getPEWorkflowDAG())) {
            throw new OmsException("illegal DAG");
        }

        Long wfId = req.getId();
        WorkflowInfoDO wf;
        if (wfId == null) {
            wf = new WorkflowInfoDO();
            wf.setGmtCreate(new Date());
        }else {
            wf = workflowInfoRepository.findById(wfId).orElseThrow(() -> new IllegalArgumentException("can't find workflow by id:" + wfId));
        }

        BeanUtils.copyProperties(req, wf);
        wf.setGmtModified(new Date());
        wf.setPeDAG(JsonUtils.toJSONString(req.getPEWorkflowDAG()));
        wf.setStatus(SwitchableStatus.valueOf(req.getStatus()).getV());
        wf.setTimeExpressionType(TimeExpressionType.valueOf(req.getTimeExpressionType()).getV());

        wf.setNotifyUserIds(SJ.commaJoiner.join(req.getNotifyUserIds()));

        // 计算 NextTriggerTime
        TimeExpressionType timeExpressionType = TimeExpressionType.valueOf(req.getTimeExpressionType());
        if (timeExpressionType == TimeExpressionType.CRON) {
            CronExpression cronExpression = new CronExpression(req.getTimeExpression());
            Date nextValidTime = cronExpression.getNextValidTimeAfter(new Date());
            wf.setNextTriggerTime(nextValidTime.getTime());
        }

        WorkflowInfoDO newEntity = workflowInfoRepository.saveAndFlush(wf);
        return newEntity.getId();
    }

    /**
     * 删除工作流（软删除）
     * @param wfId 工作流ID
     * @param appId 所属应用ID
     */
    public void deleteWorkflow(Long wfId, Long appId) {
        WorkflowInfoDO wfInfo = permissionCheck(wfId, appId);
        wfInfo.setStatus(SwitchableStatus.DELETED.getV());
        wfInfo.setGmtModified(new Date());
        workflowInfoRepository.saveAndFlush(wfInfo);
    }

    /**
     * 禁用工作流
     * @param wfId 工作流ID
     * @param appId 所属应用ID
     */
    public void disableWorkflow(Long wfId, Long appId) {
        WorkflowInfoDO wfInfo = permissionCheck(wfId, appId);
        wfInfo.setStatus(SwitchableStatus.DISABLE.getV());
        wfInfo.setGmtModified(new Date());
        workflowInfoRepository.saveAndFlush(wfInfo);
    }

    /**
     * 立即运行工作流
     * @param wfId 工作流ID
     * @param appId 所属应用ID
     * @return 该 workflow 实例的 instanceId（wfInstanceId）
     */
    public Long runWorkflow(Long wfId, Long appId) {

        WorkflowInfoDO wfInfo = permissionCheck(wfId, appId);
        Long wfInstanceId = workflowInstanceManager.create(wfInfo);

        // 正式启动任务
        workflowInstanceManager.start(wfInfo, wfInstanceId);
        return wfInstanceId;
    }

    private WorkflowInfoDO permissionCheck(Long wfId, Long appId) {
        WorkflowInfoDO wfInfo = workflowInfoRepository.findById(wfId).orElseThrow(() -> new IllegalArgumentException("can't find workflow by id: " + wfId));
        if (!wfInfo.getAppId().equals(appId)) {
            throw new OmsException("Permission Denied!can't delete other appId's workflow!");
        }
        return wfInfo;
    }
}
