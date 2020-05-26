package com.github.kfcfans.oms.server.service.workflow;

import com.github.kfcfans.oms.common.OmsException;
import com.github.kfcfans.oms.common.TimeExpressionType;
import com.github.kfcfans.oms.common.request.http.SaveWorkflowRequest;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.common.utils.WorkflowDAGUtils;
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

        // 计算 NextTriggerTime
        TimeExpressionType timeExpressionType = TimeExpressionType.of(req.getTimeExpressionType());
        if (timeExpressionType == TimeExpressionType.CRON) {
            CronExpression cronExpression = new CronExpression(req.getTimeExpression());
            Date nextValidTime = cronExpression.getNextValidTimeAfter(new Date());
            wf.setNextTriggerTime(nextValidTime.getTime());
        }

        WorkflowInfoDO newEntity = workflowInfoRepository.saveAndFlush(wf);
        return newEntity.getId();
    }

}
