package tech.powerjob.server.web.response;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.model.LifeCycle;
import tech.powerjob.common.model.PEWorkflowDAG;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;
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

    /**
     * 所属应用ID
     */
    private Long appId;

    /**
     * 点线表示法
     */
    private PEWorkflowDAG pEWorkflowDAG;

    /* ************************** 定时参数 ************************** */

    /**
     * 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
     */
    private String timeExpressionType;
    /**
     * 时间表达式，CRON/NULL/LONG/LONG
     */
    private String timeExpression;

    /**
     * 最大同时运行的工作流个数，默认 1
     */
    private Integer maxWfInstanceNum;

    /**
     * ENABLE / DISABLE
     */
    private Boolean enable;
    /**
     * 工作流整体失败的报警
     */
    private List<Long> notifyUserIds;

    private LifeCycle lifeCycle;

    private Date gmtCreate;

    private Date gmtModified;

    public static WorkflowInfoVO from(WorkflowInfoDO wfDO) {
        WorkflowInfoVO vo = new WorkflowInfoVO();
        BeanUtils.copyProperties(wfDO, vo);

        vo.enable = SwitchableStatus.of(wfDO.getStatus()) == SwitchableStatus.ENABLE;
        vo.setTimeExpressionType(TimeExpressionType.of(wfDO.getTimeExpressionType()).name());
        vo.setPEWorkflowDAG(JSON.parseObject(wfDO.getPeDAG(), PEWorkflowDAG.class));
        if (!StringUtils.isEmpty(wfDO.getNotifyUserIds())) {
            vo.setNotifyUserIds(SJ.COMMA_SPLITTER.splitToList(wfDO.getNotifyUserIds()).stream().map(Long::valueOf).collect(Collectors.toList()));
        }
        if (!StringUtils.isEmpty(wfDO.getLifecycle())) {
            vo.lifeCycle = LifeCycle.parse(wfDO.getLifecycle());
        }
        return vo;
    }
}
