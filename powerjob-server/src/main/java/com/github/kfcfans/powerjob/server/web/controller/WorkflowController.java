package com.github.kfcfans.powerjob.server.web.controller;

import com.github.kfcfans.powerjob.common.request.http.SaveWorkflowRequest;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.server.common.constans.SwitchableStatus;
import com.github.kfcfans.powerjob.server.persistence.PageResult;
import com.github.kfcfans.powerjob.server.persistence.core.model.WorkflowInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.WorkflowInfoRepository;
import com.github.kfcfans.powerjob.server.service.workflow.WorkflowService;
import com.github.kfcfans.powerjob.server.web.request.QueryWorkflowInfoRequest;
import com.github.kfcfans.powerjob.server.web.response.WorkflowInfoVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * 工作流控制器
 *
 * @author tjq
 * @since 2020/5/26
 */
@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    @Resource
    private WorkflowService workflowService;
    @Resource
    private WorkflowInfoRepository workflowInfoRepository;

    @PostMapping("/save")
    public ResultDTO<Long> save(@RequestBody SaveWorkflowRequest req) throws Exception {
        return ResultDTO.success(workflowService.saveWorkflow(req));
    }

    @GetMapping("/disable")
    public ResultDTO<Void> disableWorkflow(Long workflowId, Long appId) {
        workflowService.disableWorkflow(workflowId, appId);
        return ResultDTO.success(null);
    }

    @GetMapping("/enable")
    public ResultDTO<Void> enableWorkflow(Long workflowId, Long appId) {
        workflowService.enableWorkflow(workflowId, appId);
        return ResultDTO.success(null);
    }

    @GetMapping("/delete")
    public ResultDTO<Void> deleteWorkflow(Long workflowId, Long appId) {
        workflowService.deleteWorkflow(workflowId, appId);
        return ResultDTO.success(null);
    }

    @PostMapping("/list")
    public ResultDTO<PageResult<WorkflowInfoVO>> list(@RequestBody QueryWorkflowInfoRequest req) {

        Sort sort = Sort.by(Sort.Direction.DESC, "gmtCreate");
        PageRequest pageRequest = PageRequest.of(req.getIndex(), req.getPageSize(), sort);
        Page<WorkflowInfoDO> wfPage;

        // 排除已删除数据
        int nStatus = SwitchableStatus.DELETED.getV();
        // 无查询条件，查询全部
        if (req.getWorkflowId() == null && StringUtils.isEmpty(req.getKeyword())) {
            wfPage = workflowInfoRepository.findByAppIdAndStatusNot(req.getAppId(), nStatus, pageRequest);
        }else if (req.getWorkflowId() != null) {
            wfPage = workflowInfoRepository.findByIdAndStatusNot(req.getWorkflowId(), nStatus, pageRequest);
        }else {
            String condition = "%" + req.getKeyword() + "%";
            wfPage = workflowInfoRepository.findByAppIdAndStatusNotAndWfNameLike(req.getAppId(), nStatus, condition, pageRequest);
        }
        return ResultDTO.success(convertPage(wfPage));
    }

    @GetMapping("/run")
    public ResultDTO<Long> runWorkflow(Long workflowId, Long appId) {
        return ResultDTO.success(workflowService.runWorkflow(workflowId, appId, null, 0L));
    }

    private static PageResult<WorkflowInfoVO> convertPage(Page<WorkflowInfoDO> originPage) {

        PageResult<WorkflowInfoVO> newPage = new PageResult<>(originPage);
        newPage.setData(originPage.getContent().stream().map(WorkflowInfoVO::from).collect(Collectors.toList()));
        return newPage;
    }

}
