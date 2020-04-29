package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.response.ResultDTO;
import com.github.kfcfans.common.model.InstanceDetail;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.persistence.PageResult;
import com.github.kfcfans.oms.server.persistence.core.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.oms.server.service.CacheService;
import com.github.kfcfans.oms.server.service.InstanceLogService;
import com.github.kfcfans.oms.server.service.instance.InstanceManager;
import com.github.kfcfans.oms.server.service.instance.InstanceService;
import com.github.kfcfans.oms.server.web.request.QueryInstanceRequest;
import com.github.kfcfans.oms.server.web.response.InstanceLogVO;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 任务实例 Controller
 *
 * @author tjq
 * @since 2020/4/9
 */
@RestController
@RequestMapping("/instance")
public class InstanceController {

    @Value("${server.port}")
    private int port;

    @Resource
    private InstanceService instanceService;
    @Resource
    private InstanceLogService instanceLogService;

    @Resource
    private CacheService cacheService;
    @Resource
    private AppInfoRepository appInfoRepository;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @GetMapping("/stop")
    public ResultDTO<Void> stopInstance(Long instanceId) {
        instanceService.stopInstance(instanceId);
        return ResultDTO.success(null);
    }

    @GetMapping("/status")
    public ResultDTO<InstanceDetail> getRunningStatus(String instanceId) {
        return ResultDTO.success(instanceService.getInstanceDetail(Long.valueOf(instanceId)));
    }

    @GetMapping("/log")
    public ResultDTO<String> getInstanceLog(Long instanceId, HttpServletResponse response) {

        InstanceInfoDO instanceInfo = instanceInfoRepository.findByInstanceId(instanceId);
        if (instanceInfo == null) {
            return ResultDTO.failed("invalid instanceId: " + instanceId);
        }

        Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(instanceInfo.getAppId());
        if (!appInfoOpt.isPresent()) {
            return ResultDTO.failed("impossible");
        }

        String targetServer = appInfoOpt.get().getCurrentServer();

        // 转发HTTP请求
        if (!OhMyServer.getActorSystemAddress().equals(targetServer)) {
            String ip = targetServer.split(":")[0];
            String url = "http://" + ip + ":" + port + "/instance/log?instanceId=" + instanceId;
            try {
                response.sendRedirect(url);
                return ResultDTO.success("redirecting...");
            }catch (Exception e) {
                return ResultDTO.failed(e);
            }
        }

        return ResultDTO.success(instanceLogService.fetchInstanceLog(instanceId));
    }

    @PostMapping("/list")
    public ResultDTO<PageResult<InstanceLogVO>> list(@RequestBody QueryInstanceRequest request) {

        Sort sort = Sort.by(Sort.Direction.DESC, "gmtModified");
        PageRequest pageable = PageRequest.of(request.getIndex(), request.getPageSize(), sort);

        // 查询全部数据
        if (request.getJobId() == null && request.getInstanceId() == null) {
            return ResultDTO.success(convertPage(instanceInfoRepository.findByAppId(request.getAppId(), pageable)));
        }

        // 根据JobId查询
        if (request.getJobId() != null) {
            return ResultDTO.success(convertPage(instanceInfoRepository.findByJobId(request.getJobId(), pageable)));
        }

        // 根据InstanceId查询
        return ResultDTO.success(convertPage(instanceInfoRepository.findByInstanceId(request.getInstanceId(), pageable)));
    }

    private PageResult<InstanceLogVO> convertPage(Page<InstanceInfoDO> page) {
        List<InstanceLogVO> content = page.getContent().stream().map(instanceLogDO -> {
            InstanceLogVO instanceLogVO = new InstanceLogVO();
            BeanUtils.copyProperties(instanceLogDO, instanceLogVO);

            // 状态转化为中文
            instanceLogVO.setStatusStr(InstanceStatus.of(instanceLogDO.getStatus()).getDes());
            // 额外设置任务名称，提高可读性
            instanceLogVO.setJobName(cacheService.getJobName(instanceLogDO.getJobId()));

            // ID 转化为 String（JS精度丢失）
            instanceLogVO.setJobId(instanceLogDO.getJobId().toString());
            instanceLogVO.setInstanceId(instanceLogDO.getInstanceId().toString());

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
