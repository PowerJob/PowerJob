package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.oms.common.InstanceStatus;
import com.github.kfcfans.oms.common.response.ResultDTO;
import com.github.kfcfans.oms.common.model.InstanceDetail;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.common.utils.OmsFileUtils;
import com.github.kfcfans.oms.server.persistence.PageResult;
import com.github.kfcfans.oms.server.persistence.StringPage;
import com.github.kfcfans.oms.server.persistence.core.model.AppInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.AppInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.oms.server.service.CacheService;
import com.github.kfcfans.oms.server.service.InstanceLogService;
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
import java.io.*;
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
    public ResultDTO<StringPage> getInstanceLog(Long instanceId, Long index, HttpServletResponse response) {

        String targetServer = getTargetServer(instanceId);

        // 转发HTTP请求（如果使用Akka，则需要传输两次，而转发HTTP请求只需要传输一次"大"数据包）
        if (!OhMyServer.getActorSystemAddress().equals(targetServer)) {
            String ip = targetServer.split(":")[0];
            String url = String.format("http://%s:%s/instance/log?instanceId=%d&index=%d", ip, port, instanceId, index);
            try {
                response.sendRedirect(url);
                return ResultDTO.success(StringPage.simple("redirecting..."));
            }catch (Exception e) {
                return ResultDTO.failed(e);
            }
        }

        return ResultDTO.success(instanceLogService.fetchInstanceLog(instanceId, index));
    }

    @GetMapping("/downloadLogUrl")
    public ResultDTO<String> getDownloadUrl(Long instanceId) {
        String targetServer = getTargetServer(instanceId);
        String ip = targetServer.split(":")[0];
        String url = "http://" + ip + ":" + port + "/instance/downloadLog?instanceId=" + instanceId;
        return ResultDTO.success(url);
    }

    @GetMapping("/downloadLog")
    public void downloadLogFile(Long instanceId , HttpServletResponse response) throws Exception {

        File file = instanceLogService.downloadInstanceLog(instanceId);
        OmsFileUtils.file2HttpResponse(file, response);
    }

    @PostMapping("/list")
    public ResultDTO<PageResult<InstanceLogVO>> list(@RequestBody QueryInstanceRequest request) {

        Sort sort = Sort.by(Sort.Direction.DESC, "gmtModified");
        PageRequest pageable = PageRequest.of(request.getIndex(), request.getPageSize(), sort);

        // 查询全部数据
        if (request.getJobId() == null && request.getInstanceId() == null) {
            return ResultDTO.success(convertPage(instanceInfoRepository.findByAppIdAndType(request.getAppId(), request.getType().getV(), pageable)));
        }

        // 根据JobId查询
        if (request.getJobId() != null) {
            return ResultDTO.success(convertPage(instanceInfoRepository.findByJobIdAndType(request.getJobId(), request.getType().getV(), pageable)));
        }

        // 根据InstanceId查询
        return ResultDTO.success(convertPage(instanceInfoRepository.findByInstanceIdAndType(request.getInstanceId(), request.getType().getV(), pageable)));
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

    /**
     * 获取该 instanceId 对应的服务器地址
     * @param instanceId 任务实例ID
     * @return 对应服务器地址
     */
    private String getTargetServer(Long instanceId) {
        InstanceInfoDO instanceInfo = instanceInfoRepository.findByInstanceId(instanceId);
        if (instanceInfo == null) {
            throw new RuntimeException("invalid instanceId: " + instanceId);
        }

        Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(instanceInfo.getAppId());
        return appInfoOpt.orElseThrow(() -> new RuntimeException("impossible")).getCurrentServer();
    }
}
