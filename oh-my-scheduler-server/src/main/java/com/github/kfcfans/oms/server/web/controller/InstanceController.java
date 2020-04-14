package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.common.model.InstanceDetail;
import com.github.kfcfans.oms.server.persistence.PageResult;
import com.github.kfcfans.oms.server.persistence.model.InstanceLogDO;
import com.github.kfcfans.oms.server.persistence.repository.InstanceLogRepository;
import com.github.kfcfans.oms.server.service.CacheService;
import com.github.kfcfans.oms.server.service.instance.InstanceService;
import com.github.kfcfans.oms.server.web.request.QueryInstanceRequest;
import com.github.kfcfans.oms.server.web.response.InstanceLogVO;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

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
    @Resource
    private CacheService cacheService;
    @Resource
    private InstanceLogRepository instanceLogRepository;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @GetMapping("/stop")
    public ResultDTO<Void> stopInstance(Long instanceId) {
        instanceService.stopInstance(instanceId);
        return ResultDTO.success(null);
    }

    @GetMapping("/status")
    public ResultDTO<InstanceDetail> getRunningStatus(Long instanceId) {
        return ResultDTO.success(instanceService.getInstanceDetail(instanceId));
    }

    @PostMapping("/list")
    public ResultDTO<PageResult<InstanceLogVO>> list(@RequestBody QueryInstanceRequest request) {

        Sort sort = Sort.by(Sort.Direction.DESC, "gmtModified");
        PageRequest pageable = PageRequest.of(request.getIndex(), request.getPageSize(), sort);

        // 查询全部数据
        if (request.getJobId() == null && request.getInstanceId() == null) {
            return ResultDTO.success(convertPage(instanceLogRepository.findByAppId(request.getAppId(), pageable)));
        }

        // 根据JobId查询
        if (request.getJobId() != null) {
            return ResultDTO.success(convertPage(instanceLogRepository.findByJobId(request.getJobId(), pageable)));
        }

        // 根据InstanceId查询
        return ResultDTO.success(convertPage(instanceLogRepository.findByInstanceId(request.getInstanceId(), pageable)));
    }

    private PageResult<InstanceLogVO> convertPage(Page<InstanceLogDO> page) {
        List<InstanceLogVO> content = page.getContent().stream().map(instanceLogDO -> {
            InstanceLogVO instanceLogVO = new InstanceLogVO();
            BeanUtils.copyProperties(instanceLogDO, instanceLogVO);

            // 状态转化为中文
            instanceLogVO.setStatus(InstanceStatus.of(instanceLogDO.getStatus()).getDes());
            // 额外设置任务名称，提高可读性
            instanceLogVO.setJobName(cacheService.getJobName(instanceLogDO.getJobId()));

            // 格式化时间
            if (instanceLogDO.getActualTriggerTime() == null) {
                instanceLogVO.setActualTriggerTime("N/A");
            }else {
                instanceLogVO.setActualTriggerTime(DateFormatUtils.format(instanceLogDO.getActualTriggerTime(), TIME_PATTERN));
            }
            if (instanceLogDO.getFinishedTime() == null) {
                instanceLogVO.setFinishedTime("N/A");
            }else {
                instanceLogVO.setFinishedTime(DateFormatUtils.format(instanceLogDO.getFinishedTime(), TIME_PATTERN));
            }

            return instanceLogVO;
        }).collect(Collectors.toList());

        PageResult<InstanceLogVO> pageResult = new PageResult<>(page);
        pageResult.setData(content);
        return pageResult;
    }
}
