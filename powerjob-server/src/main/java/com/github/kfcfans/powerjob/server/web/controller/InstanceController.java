package com.github.kfcfans.powerjob.server.web.controller;

import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.server.common.utils.OmsFileUtils;
import com.github.kfcfans.powerjob.server.persistence.PageResult;
import com.github.kfcfans.powerjob.server.persistence.StringPage;
import com.github.kfcfans.powerjob.server.persistence.core.model.AppInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.AppInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.powerjob.server.service.CacheService;
import com.github.kfcfans.powerjob.server.service.InstanceLogService;
import com.github.kfcfans.powerjob.server.service.instance.InstanceService;
import com.github.kfcfans.powerjob.server.web.request.QueryInstanceRequest;
import com.github.kfcfans.powerjob.server.web.response.InstanceDetailVO;
import com.github.kfcfans.powerjob.server.web.response.InstanceInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 任务实例 Controller
 *
 * @author tjq
 * @since 2020/4/9
 */
@Slf4j
@RestController
@RequestMapping("/instance")
public class InstanceController {



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

    @GetMapping("/stop")
    public ResultDTO<Void> stopInstance(Long instanceId) {
        instanceService.stopInstance(instanceId);
        return ResultDTO.success(null);
    }

    @GetMapping("/retry")
    public ResultDTO<Void> retryInstance(String appId, Long instanceId) {
        instanceService.retryInstance(Long.valueOf(appId), instanceId);
        return ResultDTO.success(null);
    }

    @GetMapping("/detail")
    public ResultDTO<InstanceDetailVO> getInstanceDetail(String instanceId) {
        return ResultDTO.success(InstanceDetailVO.from(instanceService.getInstanceDetail(Long.valueOf(instanceId))));
    }

    @GetMapping("/log")
    public ResultDTO<StringPage> getInstanceLog(Long appId, Long instanceId, Long index) {
        return ResultDTO.success(instanceLogService.fetchInstanceLog(appId, instanceId, index));
    }

    @GetMapping("/downloadLogUrl")
    public ResultDTO<String> getDownloadUrl(Long appId, Long instanceId) {
        return ResultDTO.success(instanceLogService.fetchDownloadUrl(appId, instanceId));
    }

    @GetMapping("/downloadLog")
    public void downloadLogFile(Long instanceId , HttpServletResponse response) throws Exception {

        File file = instanceLogService.downloadInstanceLog(instanceId);
        OmsFileUtils.file2HttpResponse(file, response);
    }

    @GetMapping("/downloadLog4Console")
    public void downloadLog4Console(Long appId, Long instanceId , HttpServletResponse response) throws Exception {
        // 获取内部下载链接
        String downloadUrl = instanceLogService.fetchDownloadUrl(appId, instanceId);
        // 先下载到本机
        String logFilePath = OmsFileUtils.genTemporaryWorkPath() + String.format("powerjob-%s-%s.log", appId, instanceId);
        File logFile = new File(logFilePath);

        try {
            FileUtils.copyURLToFile(new URL(downloadUrl), logFile);

            // 再推送到浏览器
            OmsFileUtils.file2HttpResponse(logFile, response);
        } finally {
            FileUtils.forceDelete(logFile);
        }
    }

    @PostMapping("/list")
    public ResultDTO<PageResult<InstanceInfoVO>> list(@RequestBody QueryInstanceRequest request) {

        Sort sort = Sort.by(Sort.Direction.DESC, "gmtModified");
        PageRequest pageable = PageRequest.of(request.getIndex(), request.getPageSize(), sort);

        InstanceInfoDO queryEntity = new InstanceInfoDO();
        BeanUtils.copyProperties(request, queryEntity);
        queryEntity.setType(request.getType().getV());

        if (!StringUtils.isEmpty(request.getStatus())) {
            queryEntity.setStatus(InstanceStatus.valueOf(request.getStatus()).getV());
        }

        Page<InstanceInfoDO> pageResult = instanceInfoRepository.findAll(Example.of(queryEntity), pageable);
        return ResultDTO.success(convertPage(pageResult));
    }

    private PageResult<InstanceInfoVO> convertPage(Page<InstanceInfoDO> page) {
        List<InstanceInfoVO> content = page.getContent().stream()
                .map(x -> InstanceInfoVO.from(x, cacheService.getJobName(x.getJobId()))).collect(Collectors.toList());

        PageResult<InstanceInfoVO> pageResult = new PageResult<>(page);
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
            throw new PowerJobException("invalid instanceId: " + instanceId);
        }

        Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(instanceInfo.getAppId());
        return appInfoOpt.orElseThrow(() -> new PowerJobException("impossible")).getCurrentServer();
    }
}
