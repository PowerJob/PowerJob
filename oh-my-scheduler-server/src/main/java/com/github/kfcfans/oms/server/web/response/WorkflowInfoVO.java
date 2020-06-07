package com.github.kfcfans.oms.server.web.response;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.oms.common.TimeExpressionType;
import com.github.kfcfans.oms.common.model.PEWorkflowDAG;
import com.github.kfcfans.oms.server.common.SJ;
import com.github.kfcfans.oms.server.common.constans.SwitchableStatus;
import com.github.kfcfans.oms.server.persistence.core.model.WorkflowInfoDO;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流对外展示对象
 *
 * @author tjq
 * @since 2020/5/27
 */
@Data
public class WorkflowInfoVO {

    private Long id;

    private String wfName;
    private String wfDescription;

    // 所属应用ID
    private Long appId;

    // 点线表示法
    private PEWorkflowDAG pEWorkflowDAG;

    /* ************************** 定时参数 ************************** */
    // 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
    private String timeExpressionType;
    // 时间表达式，CRON/NULL/LONG/LONG
    private String timeExpression;

    // 最大同时运行的工作流个数，默认 1
    private Integer maxWfInstanceNum;

    // ENABLE / DISABLE
    private boolean enable;

    // 工作流整体失败的报警
    private List<Long> notifyUserIds;

    public static WorkflowInfoVO from(WorkflowInfoDO wfDO) {
        WorkflowInfoVO vo = new WorkflowInfoVO();
        BeanUtils.copyProperties(wfDO, vo);

        vo.enable = SwitchableStatus.of(wfDO.getStatus()) == SwitchableStatus.ENABLE;
        vo.setTimeExpressionType(TimeExpressionType.of(wfDO.getTimeExpressionType()).name());
        vo.setPEWorkflowDAG(JSONObject.parseObject(wfDO.getPeDAG(), PEWorkflowDAG.class));
        if (!StringUtils.isEmpty(wfDO.getNotifyUserIds())) {
            vo.setNotifyUserIds(SJ.commaSplitter.splitToList(wfDO.getNotifyUserIds()).stream().map(Long::valueOf).collect(Collectors.toList()));
        }
        return vo;
    }
}
