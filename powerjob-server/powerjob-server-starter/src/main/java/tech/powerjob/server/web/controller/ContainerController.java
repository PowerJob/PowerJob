package tech.powerjob.server.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.common.constants.ContainerSourceType;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.common.utils.OmsFileUtils;
import tech.powerjob.server.core.container.ContainerService;
import tech.powerjob.server.core.container.ContainerTemplateGenerator;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.model.ContainerInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.persistence.remote.repository.ContainerInfoRepository;
import tech.powerjob.server.web.request.GenerateContainerTemplateRequest;
import tech.powerjob.server.web.request.SaveContainerInfoRequest;
import tech.powerjob.server.web.response.ContainerInfoVO;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 容器信息控制层
 *
 * @author tjq
 * @since 2020/5/15
 */
@Slf4j
@RestController
@RequestMapping("/container")
public class ContainerController {


    private final int port;

    private final ContainerService containerService;

    private final AppInfoRepository appInfoRepository;

    private final ContainerInfoRepository containerInfoRepository;

    public ContainerController(@Value("${server.port}") int port, ContainerService containerService, AppInfoRepository appInfoRepository, ContainerInfoRepository containerInfoRepository) {
        this.port = port;
        this.containerService = containerService;
        this.appInfoRepository = appInfoRepository;
        this.containerInfoRepository = containerInfoRepository;
    }

    @GetMapping("/downloadJar")
    public void downloadJar(String version, HttpServletResponse response) throws IOException {
        File file = containerService.fetchContainerJarFile(version);
        if (file.exists()) {
            OmsFileUtils.file2HttpResponse(file, response);
        } else {
            log.error("[Container] can't find container by version[{}], please deploy first!", version);
        }
    }

    @PostMapping("/downloadContainerTemplate")
    public void downloadContainerTemplate(@RequestBody GenerateContainerTemplateRequest req, HttpServletResponse response) throws IOException {
        File zipFile = ContainerTemplateGenerator.generate(req.getGroup(), req.getArtifact(), req.getName(), req.getPackageName(), req.getJavaVersion());
        OmsFileUtils.file2HttpResponse(zipFile, response);
    }

    @PostMapping("/jarUpload")
    public ResultDTO<String> fileUpload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResultDTO.failed("empty file");
        }
        return ResultDTO.success(containerService.uploadContainerJarFile(file));
    }

    @PostMapping("/save")
    public ResultDTO<Void> saveContainer(@RequestBody SaveContainerInfoRequest request) {
        request.valid();

        ContainerInfoDO container = new ContainerInfoDO();
        BeanUtils.copyProperties(request, container);
        container.setSourceType(request.getSourceType().getV());
        container.setStatus(request.getStatus().getV());

        containerService.save(container);
        return ResultDTO.success(null);
    }

    @GetMapping("/delete")
    public ResultDTO<Void> deleteContainer(Long appId, Long containerId) {
        containerService.delete(appId, containerId);
        return ResultDTO.success(null);
    }

    @GetMapping("/list")
    public ResultDTO<List<ContainerInfoVO>> listContainers(Long appId) {
        List<ContainerInfoVO> res = containerInfoRepository.findByAppIdAndStatusNot(appId, SwitchableStatus.DELETED.getV())
                .stream().map(ContainerController::convert).collect(Collectors.toList());
        return ResultDTO.success(res);
    }

    @GetMapping("/listDeployedWorker")
    public ResultDTO<String> listDeployedWorker(Long appId, Long containerId, HttpServletResponse response) {
        AppInfoDO appInfoDO = appInfoRepository.findById(appId).orElseThrow(() -> new IllegalArgumentException("can't find app by id:" + appId));
        String targetServer = appInfoDO.getCurrentServer();

        if (StringUtils.isEmpty(targetServer)) {
            return ResultDTO.failed("No workers have even registered！");
        }

        return ResultDTO.success(containerService.fetchDeployedInfo(appId, containerId));
    }

    private static ContainerInfoVO convert(ContainerInfoDO containerInfoDO) {
        ContainerInfoVO vo = new ContainerInfoVO();
        BeanUtils.copyProperties(containerInfoDO, vo);
        if (containerInfoDO.getLastDeployTime() == null) {
            vo.setLastDeployTime("N/A");
        }else {
            vo.setLastDeployTime(DateFormatUtils.format(containerInfoDO.getLastDeployTime(), OmsConstant.TIME_PATTERN));
        }
        SwitchableStatus status = SwitchableStatus.of(containerInfoDO.getStatus());
        vo.setStatus(status.name());
        ContainerSourceType sourceType = ContainerSourceType.of(containerInfoDO.getSourceType());
        vo.setSourceType(sourceType.name());
        return vo;
    }
}
