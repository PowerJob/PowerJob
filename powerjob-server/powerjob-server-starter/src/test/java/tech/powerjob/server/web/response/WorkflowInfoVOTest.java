package tech.powerjob.server.web.response;

import org.junit.jupiter.api.Test;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.server.persistence.remote.model.WorkflowInfoDO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * WorkflowInfoVO 单元测试
 *
 * @author cgrs5722
 * @since 2026/10/20
 */
class WorkflowInfoVOTest {

    /**
     * 构造仅包含 from() 必需字段的 DO
     */
    private WorkflowInfoDO buildDO(Integer status) {
        WorkflowInfoDO wfDO = new WorkflowInfoDO();
        wfDO.setId(1L);
        wfDO.setWfName("test-wf");
        wfDO.setStatus(status);
        wfDO.setTimeExpressionType(TimeExpressionType.CRON.getV());
        return wfDO;
    }

    /**
     * from() 应将 DO 的 status 拷贝到 VO，供 API 响应返回
     */
    @Test
    void fromShouldCarryStatusField() {
        WorkflowInfoVO vo = WorkflowInfoVO.from(buildDO(SwitchableStatus.ENABLE.getV()));
        assertNotNull(vo);
        assertEquals(SwitchableStatus.ENABLE.getV(), vo.getStatus());
        assertEquals(Boolean.TRUE, vo.getEnable());

        WorkflowInfoVO disabledVO = WorkflowInfoVO.from(buildDO(SwitchableStatus.DISABLE.getV()));
        assertEquals(SwitchableStatus.DISABLE.getV(), disabledVO.getStatus());
        assertEquals(Boolean.FALSE, disabledVO.getEnable());
    }
}
