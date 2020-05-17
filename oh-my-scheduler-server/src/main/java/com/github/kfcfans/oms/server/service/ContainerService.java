package com.github.kfcfans.oms.server.service;

import com.github.kfcfans.oms.common.model.GitRepoInfo;
import com.github.kfcfans.oms.common.utils.CommonUtils;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.server.common.constans.ContainerSourceType;
import com.github.kfcfans.oms.server.common.utils.OmsFileUtils;
import com.github.kfcfans.oms.server.persistence.core.model.ContainerInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.ContainerInfoRepository;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.Optional;

/**
 * 容器服务
 *
 * @author tjq
 * @since 2020/5/16
 */
@Slf4j
@Service
public class ContainerService {

    @Resource
    private ContainerInfoRepository containerInfoRepository;

    private GridFsTemplate gridFsTemplate;

    /**
     * 获取构建容器所需要的 Jar 文件
     * @param md5 Jar文件的MD5值，可以由此构建 mongoDB 文件名
     * @return 本地Jar文件
     */
    public File fetchContainerJarFile(String md5) {

        String jarFileName = OmsFileUtils.genContainerJarPath() + genContainerJarName(md5);
        File jarFile = new File(jarFileName);

        if (jarFile.exists()) {
            return jarFile;
        }
        if (gridFsTemplate != null) {
            downloadJarFromGridFS(genContainerJarName(md5), jarFile);
        }
        return jarFile;
    }

    /**
     * 部署容器
     * @param containerName 容器名称
     */
    public void deploy(String containerName) throws Exception {
        Optional<ContainerInfoDO> containerInfoOpt = containerInfoRepository.findByContainerName(containerName);
        ContainerInfoDO container = containerInfoOpt.orElseThrow(() -> new IllegalArgumentException("can't find container by name: "+ containerName));

        // 获取Jar，Git需要先 clone成Jar计算MD5，JarFile则直接下载
        ContainerSourceType sourceType = ContainerSourceType.of(container.getSourceType());
        if (sourceType == ContainerSourceType.Git) {


        }
    }

    private void downloadJarFromGridFS(String mongoFileName, File targetFile) {
        synchronized (mongoFileName.intern()) {
            if (targetFile.exists()) {
                return;
            }
            GridFsResource gridFsResource = gridFsTemplate.getResource(mongoFileName);
            if (!gridFsResource.exists()) {
                log.warn("[ContainerService] can't find container's jar file({}) in gridFS.", mongoFileName);
                return;
            }
            try {
                OmsFileUtils.gridFs2File(gridFsResource, targetFile);
            }catch (Exception e) {
                CommonUtils.executeIgnoreException(() -> FileUtils.forceDelete(targetFile));
                throw e;
            }
        }
    }

    private static String genContainerJarName(String md5) {
        return String.format("oms-container-%s.jar", md5);
    }

    @Autowired(required = false)
    public void setGridFsTemplate(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    public static void main(String[] args) throws Exception {

        String gitRepoInfoStr = "{\"repo\":\"https://gitee.com/KFCFans/OhMyScheduler-Container-Template.git\",\"branch\":\"master\"}";

        String workerDirStr = OmsFileUtils.genTemporaryPath();
        File workerDir = new File(workerDirStr);
        FileUtils.forceMkdir(workerDir);

        // git clone
        GitRepoInfo gitRepoInfo = JsonUtils.parseObject(gitRepoInfoStr, GitRepoInfo.class);

        CloneCommand cloneCommand = Git.cloneRepository()
                .setDirectory(workerDir)
                .setURI(gitRepoInfo.getRepo())
                .setBranch(gitRepoInfo.getBranch());
        if (!StringUtils.isEmpty(gitRepoInfo.getUsername())) {
            CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(gitRepoInfo.getUsername(), gitRepoInfo.getPassword());
            cloneCommand.setCredentialsProvider(credentialsProvider);
        }
        cloneCommand.call();

        // mvn clean package -DskipTests -U
        Invoker mvnInvoker = new DefaultInvoker();
        InvocationRequest ivkReq = new DefaultInvocationRequest();
        ivkReq.setGoals(Lists.newArrayList("clean", "package", "-DskipTests", "-U"));
        ivkReq.setBaseDirectory(workerDir);
        ivkReq.setOutputHandler(line -> {
            System.out.println(line);
        });

        InvocationResult mvnResult = mvnInvoker.execute(ivkReq);
        if (mvnResult.getExitCode() != 0) {
            // TODO：输出失败信息
            return;
        }

        String targetDirStr = workerDirStr + "/target";
        File targetDir = new File(targetDirStr);
        IOFileFilter fileFilter = FileFilterUtils.asFileFilter((dir, name) -> name.endsWith("jar-with-dependencies.jar"));
        Collection<File> jarFile = FileUtils.listFiles(targetDir, fileFilter, null);

        if (CollectionUtils.isEmpty(jarFile)) {
            // TODO：输出失败信息
            return;
        }

        File jarWithDependency = jarFile.iterator().next();

    }
}
