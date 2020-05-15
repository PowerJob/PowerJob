package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.oms.common.response.ResultDTO;
import com.github.kfcfans.oms.server.common.constans.ContainerSourceType;
import com.github.kfcfans.oms.server.common.constans.ContainerStatus;
import com.github.kfcfans.oms.server.common.utils.OmsFilePathUtils;
import com.github.kfcfans.oms.server.persistence.core.model.ContainerInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.ContainerInfoRepository;
import com.github.kfcfans.oms.server.web.request.GenerateContainerTemplateRequest;
import com.github.kfcfans.oms.server.web.request.SaveContainerInfoRequest;
import com.github.kfcfans.oms.server.web.response.ContainerInfoVO;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
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
import java.net.URLEncoder;
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
public class ContainerController {

    private GridFsTemplate gridFsTemplate;

    @Resource
    private ContainerInfoRepository containerInfoRepository;

    @PostMapping("/downloadContainerTemplate")
    public void downloadContainerTemplate(@RequestBody GenerateContainerTemplateRequest request, HttpServletResponse response) throws Exception {

        String fileName = request.getName() + ".zip";

        // TODO：模版类下载

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        response.getOutputStream().write("mockmockmockmockmockmock".getBytes());
    }

    @PostMapping("/jarUpload")
    public ResultDTO<String> fileUpload(@RequestParam("file")MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResultDTO.failed("empty file");
        }

        // 1. 本地持久化
        String path = OmsFilePathUtils.genContainerJarPath();
        String tmpFileName = UUID.randomUUID().toString() + ".jar";
        tmpFileName = StringUtils.replace(tmpFileName, "-", "");
        File jarFile = new File(path + tmpFileName);
        OmsFilePathUtils.forceMkdir(jarFile);

        file.transferTo(jarFile);
        log.debug("[ContainerController] upload jarFile({}) to local disk success.", tmpFileName);

        // 2. 检查是否符合标准（是否为Jar，是否符合 template）

        // 3. 生成MD5
        String realFileName;
        try(FileInputStream fis = new FileInputStream(jarFile)) {
            realFileName = DigestUtils.md5DigestAsHex(fis);
        }

        // 3. 推送到 mongoDB
        if (gridFsTemplate != null) {
        }

        return ResultDTO.success(realFileName);
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
            containerInfoDO.setFileName(request.getSourceInfo());
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
