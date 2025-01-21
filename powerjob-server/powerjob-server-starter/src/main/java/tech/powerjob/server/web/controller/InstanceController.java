package tech.powerjob.server.web.controller;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.common.utils.HttpHeaderUtils;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.common.utils.OmsFileUtils;
import tech.powerjob.server.core.instance.InstanceLogService;
import tech.powerjob.server.core.instance.InstanceService;
import tech.powerjob.server.core.service.CacheService;
import tech.powerjob.server.persistence.PageResult;
import tech.powerjob.server.persistence.StringPage;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.repository.InstanceInfoRepository;
import tech.powerjob.server.web.request.QueryInstanceDetailRequest;
import tech.powerjob.server.web.request.QueryInstanceRequest;
import tech.powerjob.server.web.response.InstanceDetailVO;
import tech.powerjob.server.web.response.InstanceInfoVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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
    private InstanceInfoRepository instanceInfoRepository;

    @GetMapping("/stop")
    @ApiPermission(name = "Instance-Stop", roleScope = RoleScope.APP, requiredPermission = Permission.OPS)
    public ResultDTO<Void> stopInstance(Long instanceId, HttpServletRequest hsr) {
        preCheck(instanceId, hsr);

        Long appId = Long.valueOf(HttpHeaderUtils.fetchAppId(hsr));
        instanceService.stopInstance(appId, instanceId);

        return ResultDTO.success(null);
    }

    @GetMapping("/retry")
    @ApiPermission(name = "Instance-Retry", roleScope = RoleScope.APP, requiredPermission = Permission.OPS)
    public ResultDTO<Void> retryInstance(Long instanceId, HttpServletRequest hsr) {

        preCheck(instanceId, hsr);

        Long appId = Long.valueOf(HttpHeaderUtils.fetchAppId(hsr));
        instanceService.retryInstance(appId, instanceId);
        return ResultDTO.success(null);
    }

    @GetMapping("/detail")
    @ApiPermission(name = "Instance-Detail", roleScope = RoleScope.APP, requiredPermission = Permission.READ)
    public ResultDTO<InstanceDetailVO> getInstanceDetail(Long instanceId, HttpServletRequest hsr) {
        QueryInstanceDetailRequest queryInstanceDetailRequest = new QueryInstanceDetailRequest();
        queryInstanceDetailRequest.setAppId(Long.valueOf(HttpHeaderUtils.fetchAppId(hsr)));
        queryInstanceDetailRequest.setInstanceId(instanceId);
        return getInstanceDetailPlus(queryInstanceDetailRequest, hsr);
    }

    @PostMapping("/detailPlus")
    @ApiPermission(name = "Instance-DetailPlus", roleScope = RoleScope.APP, requiredPermission = Permission.READ)
    public ResultDTO<InstanceDetailVO> getInstanceDetailPlus(@RequestBody QueryInstanceDetailRequest req, HttpServletRequest hsr) {

        // 非法请求参数校验
        String customQuery = req.getCustomQuery();
        String nonNullCustomQuery = Optional.ofNullable(customQuery).orElse(OmsConstant.NONE);
        if (StringUtils.containsAnyIgnoreCase(nonNullCustomQuery, "delete", "update", "insert", "drop", "CREATE", "ALTER", "TRUNCATE", "RENAME", "LOCK", "GRANT", "REVOKE", "PREPARE", "EXECUTE", "COMMIT", "BEGIN")) {
            throw new IllegalArgumentException("Don't get any ideas about the database, illegally query: " + customQuery);
        }

        // 兼容老版本前端不存在 appId 的场景
        if (req.getAppId() == null) {
            req.setAppId(instanceService.getInstanceInfo(req.getInstanceId()).getAppId());
        } else {
            req.setAppId(Long.valueOf(HttpHeaderUtils.fetchAppId(hsr)));
        }

        return ResultDTO.success(InstanceDetailVO.from(instanceService.getInstanceDetail(req.getAppId(), req.getInstanceId(), customQuery)));
    }

    @GetMapping("/log")
    @ApiPermission(name = "Instance-Log", roleScope = RoleScope.APP, requiredPermission = Permission.OPS)
    public ResultDTO<StringPage> getInstanceLog(Long instanceId, Long index, HttpServletRequest hsr) {

        preCheck(instanceId, hsr);
        Long appId = Long.valueOf(HttpHeaderUtils.fetchAppId(hsr));
        return ResultDTO.success(instanceLogService.fetchInstanceLog(appId, instanceId, index));
    }

    @GetMapping("/downloadLogUrl")
    @ApiPermission(name = "Instance-FetchDownloadLogUrl", roleScope = RoleScope.APP, requiredPermission = Permission.READ)
    public ResultDTO<String> getDownloadUrl(Long instanceId, HttpServletRequest hsr) {

        preCheck(instanceId, hsr);
        Long appId = Long.valueOf(HttpHeaderUtils.fetchAppId(hsr));

        return ResultDTO.success(instanceLogService.fetchDownloadUrl(appId, instanceId));
    }

    @GetMapping("/downloadLog")
    public void downloadLogFile(Long instanceId , HttpServletResponse response) throws Exception {

        File file = instanceLogService.downloadInstanceLog(instanceId);
        OmsFileUtils.file2HttpResponse(file, response);
    }

    @GetMapping("/downloadLog4Console")
    @SneakyThrows
    public void downloadLog4Console(Long appId, Long instanceId , HttpServletResponse response) {
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
    @ApiPermission(name = "Instance-List", roleScope = RoleScope.APP, requiredPermission = Permission.READ)
    public ResultDTO<PageResult<InstanceInfoVO>> list(@RequestBody QueryInstanceRequest request, HttpServletRequest hsr) {

        Long appId = Long.valueOf(HttpHeaderUtils.fetchAppId(hsr));
        request.setAppId(appId);

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

    private void preCheck(Long instanceId, HttpServletRequest hsr) {
        Optional<InstanceInfoDO> instanceInfoOpt = instanceInfoRepository.findById(instanceId);
        if (!instanceInfoOpt.isPresent()) {
            throw new PowerJobException(ErrorCodes.ILLEGAL_ARGS_ERROR, "Can'tFindInstanceInfoById:" + instanceId);
        }
        Long appId = instanceInfoOpt.get().getAppId();
        String targetId = HttpHeaderUtils.fetchAppId(hsr);
        if (!targetId.equalsIgnoreCase(String.valueOf(appId))) {
            throw new PowerJobException(ErrorCodes.INVALID_REQUEST, String.format("AppIdNotMatch(%d!=%d)", targetId, appId));
        }
    }

}
