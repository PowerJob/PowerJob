package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.oms.common.response.ResultDTO;
import com.github.kfcfans.oms.server.common.constans.ContainerSourceType;
import com.github.kfcfans.oms.server.common.utils.ContainerTemplateGenerator;
import com.github.kfcfans.oms.server.common.utils.OmsFileUtils;
import com.github.kfcfans.oms.server.persistence.core.model.ContainerInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.ContainerInfoRepository;
import com.github.kfcfans.oms.server.service.ContainerService;
import com.github.kfcfans.oms.server.web.request.GenerateContainerTemplateRequest;
import com.github.kfcfans.oms.server.web.request.SaveContainerInfoRequest;
import com.github.kfcfans.oms.server.web.response.ContainerInfoVO;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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

    private GridFsTemplate gridFsTemplate;

    @Resource
    private ContainerInfoRepository containerInfoRepository;

    @Resource
    private ContainerService containerService;

    @GetMapping("/downloadJar")
    public void downloadJar(String md5, HttpServletResponse response) throws IOException {
        File file = containerService.fetchContainerJarFile(md5);
        if (file.exists()) {
            OmsFileUtils.file2HttpResponse(file, response);
        }
    }

    @PostMapping("/downloadContainerTemplate")
    public void downloadContainerTemplate(@RequestBody GenerateContainerTemplateRequest req, HttpServletResponse response) throws IOException {
        File zipFile = ContainerTemplateGenerator.generate(req.getGroup(), req.getArtifact(), req.getName(), req.getPackageName(), req.getJavaVersion());
        OmsFileUtils.file2HttpResponse(zipFile, response);
    }

    @PostMapping("/jarUpload")
    public ResultDTO<String> fileUpload(@RequestParam("file")MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResultDTO.failed("empty file");
        }

        // 1. 本地持久化
        String path = OmsFileUtils.genContainerJarPath();
        String tmpFileName = UUID.randomUUID().toString() + ".jar";
        tmpFileName = StringUtils.replace(tmpFileName, "-", "");
        File jarFile = new File(path + tmpFileName);
        FileUtils.forceMkdirParent(jarFile);

        file.transferTo(jarFile);
        log.debug("[ContainerController] upload jarFile({}) to local disk success.", tmpFileName);

        // 2. 检查是否符合标准（是否为Jar，是否符合 template）

        // 3. 生成MD5
        String md5;
        try(FileInputStream fis = new FileInputStream(jarFile)) {
            md5 = DigestUtils.md5DigestAsHex(fis);
        }

        // 3. 推送到 mongoDB
        if (gridFsTemplate != null) {
        }

        return ResultDTO.success(md5);
    }

    @PostMapping("/save")
    public ResultDTO<Void> saveContainer(@RequestBody SaveContainerInfoRequest request) {

        ContainerInfoDO containerInfoDO;
        if (request.getId() == null) {
            containerInfoDO = new ContainerInfoDO();
            containerInfoDO.setGmtModified(new Date());
        }else {
            containerInfoDO = containerInfoRepository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("can't find container by id: " + request.getId()));
        }
        BeanUtils.copyProperties(request, containerInfoDO);

        containerInfoDO.setSourceType(request.getSourceType().getV());
        containerInfoDO.setStatus(request.getStatus().getV());
        containerInfoDO.setGmtCreate(new Date());

        // git clone -> mvn clean package -> md5 生成文件名称
        if (request.getSourceType() == ContainerSourceType.Git) {

        }else {
            containerInfoDO.setMd5(request.getSourceInfo());
        }

        containerInfoRepository.saveAndFlush(containerInfoDO);
        return ResultDTO.success(null);
    }

    @GetMapping("/list")
    public ResultDTO<List<ContainerInfoVO>> listContainers(Long appId) {
        List<ContainerInfoVO> res = containerInfoRepository.findByAppId(appId).stream().map(ContainerController::convert).collect(Collectors.toList());
        return ResultDTO.success(res);
    }

    @GetMapping("/deploy")
    public ResultDTO<Void> deploy(Long containerId) {
        // TODO：最好支持显示阶段，需要问问FN怎么搞
        return ResultDTO.success(null);
    }

    @GetMapping("/listDeployedWorker")
    public ResultDTO<List<String>> listDeployedWorker(Long appId, Long containerId) {
        // TODO：本地 ContainerManager 直接返回
        List<String> mock = Lists.newArrayList("192.168.1.1:9900", "192.168.1.1:9901");
        return ResultDTO.success(mock);
    }

    @GetMapping("/delete")
    public ResultDTO<Void> deleteContainer(Long appId, Long containerId) {
        // TODO: 先停止各个Worker的容器实例
        containerInfoRepository.deleteById(containerId);
        return ResultDTO.success(null);
    }


    private static ContainerInfoVO convert(ContainerInfoDO containerInfoDO) {
        ContainerInfoVO vo = new ContainerInfoVO();
        BeanUtils.copyProperties(containerInfoDO, vo);
        return vo;
    }

    @Autowired(required = false)
    public void setGridFsTemplate(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }
}
