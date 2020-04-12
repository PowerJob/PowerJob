package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.common.model.InstanceDetail;
import com.github.kfcfans.oms.server.persistence.PageResult;
import com.github.kfcfans.oms.server.persistence.model.InstanceLogDO;
import com.github.kfcfans.oms.server.service.instance.InstanceService;
import com.github.kfcfans.oms.server.web.response.InstanceLogVO;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 任务实例 Controller
 *
 * @author tjq
 * @since 2020/4/9
 */
@RestController
@RequestMapping("/instance")
public class InstanceController {

    @Resource
    private InstanceService instanceService;

    @GetMapping("/stop")
    public ResultDTO<Void> stopInstance(Long instanceId) {
        instanceService.stopInstance(instanceId);
        return ResultDTO.success(null);
    }

    @GetMapping("/status")
    public ResultDTO<InstanceDetail> getRunningStatus(Long instanceId) {
        return ResultDTO.success(instanceService.getInstanceDetail(instanceId));
    }

    @GetMapping("/list")
    public ResultDTO<PageResult<InstanceLogVO>> list(Long appId, int index, int pageSize) {

        Page<InstanceLogDO> page = instanceService.listInstance(appId, index, pageSize);
        List<InstanceLogVO> content = page.getContent().stream().map(instanceLogDO -> {
            InstanceLogVO instanceLogVO = new InstanceLogVO();
            BeanUtils.copyProperties(instanceLogDO, instanceLogVO);
            instanceLogVO.setStatus(InstanceStatus.of(instanceLogDO.getStatus()).getDes());
            return instanceLogVO;
        }).collect(Collectors.toList());

        PageResult<InstanceLogVO> pageResult = new PageResult<>(page);
        pageResult.setData(content);
        return ResultDTO.success(pageResult);
    }
}
