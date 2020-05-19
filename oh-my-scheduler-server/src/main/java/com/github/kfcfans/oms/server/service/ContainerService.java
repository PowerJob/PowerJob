package com.github.kfcfans.oms.server.service;

import akka.actor.ActorSelection;
import com.github.kfcfans.oms.common.model.GitRepoInfo;
import com.github.kfcfans.oms.common.request.ServerDeployContainerRequest;
import com.github.kfcfans.oms.common.request.ServerDestroyContainerRequest;
import com.github.kfcfans.oms.common.utils.CommonUtils;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.common.utils.NetUtils;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.common.constans.ContainerSourceType;
import com.github.kfcfans.oms.server.common.utils.OmsFileUtils;
import com.github.kfcfans.oms.server.persistence.core.model.ContainerInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.ContainerInfoRepository;
import com.github.kfcfans.oms.server.persistence.mongodb.GridFsManager;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
import com.github.kfcfans.oms.server.service.lock.LockService;
import com.github.kfcfans.oms.server.web.request.SaveContainerInfoRequest;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.maven.shared.invoker.*;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private Environment environment;
    @Resource
    private LockService lockService;
    @Resource
    private ContainerInfoRepository containerInfoRepository;
    @Resource
    private GridFsManager gridFsManager;

    // 并发部署的机器数量
    private static final int DEPLOY_BATCH_NUM = 50;
    // 部署间隔
    private static final long DEPLOY_MIN_INTERVAL = 10 * 60 * 1000;
    // 时间格式
    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 保存容器
     * @param request 容器保存请求
     */
    public void save(SaveContainerInfoRequest request) {
        ContainerInfoDO container;
        Long originId = request.getId();
        if (originId != null) {
            container = containerInfoRepository.findById(originId).orElseThrow(() -> new IllegalArgumentException("can't find container by id: " + originId));
        }else {
            container = new ContainerInfoDO();
            container.setGmtCreate(new Date());
        }
        BeanUtils.copyProperties(request, container);
        container.setGmtModified(new Date());
        container.setSourceType(request.getSourceType().getV());
        container.setStatus(request.getStatus().getV());

        // 文件上传形式的 sourceInfo 为该文件的 md5 值，Git形式的 md5 在部署阶段生成
        if (request.getSourceType() == ContainerSourceType.JarFile) {
            container.setVersion(request.getSourceInfo());
        }else {
            container.setVersion("init");
        }
        containerInfoRepository.saveAndFlush(container);
    }

    /**
     * 删除容器（通知 Worker 销毁容器 & 删除数据库）
     * @param appId 应用ID，用于保护性判断
     * @param containerId 容器ID
     */
    public void delete(Long appId, Long containerId) {

        ContainerInfoDO container = containerInfoRepository.findById(containerId).orElseThrow(() -> new IllegalArgumentException("can't find container by id: " + containerId));

        if (!Objects.equals(appId, container.getAppId())) {
            throw new RuntimeException("Permission Denied!");
        }

        ServerDestroyContainerRequest destroyRequest = new ServerDestroyContainerRequest(container.getContainerName());
        WorkerManagerService.getActiveWorkerInfo(container.getAppId()).keySet().forEach(akkaAddress -> {
            ActorSelection workerActor = OhMyServer.getWorkerActor(akkaAddress);
            workerActor.tell(destroyRequest, null);
        });

        log.info("[ContainerService] delete container: {}.", container);
        // 硬删除算了...留着好像也没什么用
        containerInfoRepository.deleteById(containerId);
    }

    /**
     * 上传用于部署的容器的 Jar 文件
     * @param file 接受的文件
     * @return 该文件的 md5 值
     * @throws IOException 异常
     */
    public String uploadContainerJarFile(MultipartFile file) throws IOException {

        String workerDirStr = OmsFileUtils.genTemporaryWorkePath();
        String tmpFileStr = workerDirStr + "tmp.jar";

        File workerDir = new File(workerDirStr);
        File tmpFile = new File(tmpFileStr);

        try {
            // 下载到本地
            FileUtils.forceMkdirParent(tmpFile);
            file.transferTo(tmpFile);

            // 生成MD5
            String md5 = OmsFileUtils.md5(tmpFile);
            String fileName = genContainerJarName(md5);

            // 上传到 mongoDB
            gridFsManager.store(tmpFile, GridFsManager.CONTAINER_BUCKET, fileName);

            // 将文件拷贝到正确的路径
            String finalFileStr = OmsFileUtils.genContainerJarPath() + fileName;
            File finalFile = new File(finalFileStr);
            FileUtils.forceDelete(finalFile);
            FileUtils.moveFile(tmpFile, finalFile);

            return md5;

        }finally {
            CommonUtils.executeIgnoreException(() -> FileUtils.forceDelete(workerDir));
        }
    }

    /**
     * 获取构建容器所需要的 Jar 文件
     * @param version 版本
     * @return 本地Jar文件
     */
    public File fetchContainerJarFile(String version) {

        String fileName = genContainerJarName(version);
        String filePath = OmsFileUtils.genContainerJarPath() + fileName;
        File localFile = new File(filePath);

        if (localFile.exists()) {
            return localFile;
        }
        if (gridFsManager.available()) {
            downloadJarFromGridFS(fileName, localFile);
        }
        return localFile;
    }

    /**
     * 部署容器
     * @param containerName 容器名称
     * @param session WebSocket Session
     * @throws Exception 异常
     */
    public void deploy(String containerName, Session session) throws Exception {

        String deployLock = "containerDeployLock-" + containerName;
        RemoteEndpoint.Async remote = session.getAsyncRemote();
        // 最长部署时间：10分钟
        boolean lock = lockService.lock(deployLock, 10 * 60 * 1000);
        if (!lock) {
            remote.sendText("SYSTEM: acquire deploy lock failed, maybe other user is deploying, please wait until the running deploy task finished.");
            return;
        }

        try {

            Optional<ContainerInfoDO> containerInfoOpt = containerInfoRepository.findByContainerName(containerName);
            if (!containerInfoOpt.isPresent()) {
                remote.sendText("SYSTEM: can't find container by name: " + containerName);
                return;
            }
            ContainerInfoDO container = containerInfoOpt.get();

            Date lastDeployTime = container.getLastDeployTime();
            if (lastDeployTime != null) {
                if ((System.currentTimeMillis() - lastDeployTime.getTime()) < DEPLOY_MIN_INTERVAL) {
                    remote.sendText("SYSTEM: [warn] deploy too frequent, last deploy time is: " + DateFormatUtils.format(lastDeployTime, TIME_PATTERN));
                }
            }

            // 准备文件
            File jarFile = prepareJarFile(container, session);
            if (jarFile == null) {
                return;
            }

            double sizeMB = 1.0 * jarFile.length() / FileUtils.ONE_MB;
            remote.sendText(String.format("SYSTEM: the jarFile(size=%fMB) is prepared and ready to be deployed to the worker.", sizeMB));

            // 修改数据库，更新 MD5和最新部署时间
            Date now = new Date();
            container.setGmtModified(now);
            container.setLastDeployTime(now);
            containerInfoRepository.saveAndFlush(container);

            // 开始部署（需要分批进行）
            Set<String> workerAddressList = WorkerManagerService.getActiveWorkerInfo(container.getAppId()).keySet();
            if (workerAddressList.isEmpty()) {
                remote.sendText("SYSTEM: there is no worker available now, deploy failed!");
                return;
            }

            String port = environment.getProperty("local.server.port");
            String downloadURL = String.format("http://%s:%s/container/downloadJar?version=%s", NetUtils.getLocalHost(), port, container.getVersion());
            ServerDeployContainerRequest req = new ServerDeployContainerRequest(container.getId(), containerName, container.getVersion(), downloadURL);
            long sleepTime = calculateSleepTime(jarFile.length());

            AtomicInteger count = new AtomicInteger();
            workerAddressList.forEach(akkaAddress -> {
                ActorSelection workerActor = OhMyServer.getWorkerActor(akkaAddress);
                workerActor.tell(req, null);

                remote.sendText("SYSTEM: send deploy request to " + akkaAddress);

                if (count.incrementAndGet() % DEPLOY_BATCH_NUM == 0) {
                    CommonUtils.executeIgnoreException(() -> Thread.sleep(sleepTime));
                }
            });

            remote.sendText("SYSTEM: deploy finished, congratulations!");

        }finally {
            lockService.unlock(deployLock);
        }
    }

    private File prepareJarFile(ContainerInfoDO container, Session session) throws Exception {

        RemoteEndpoint.Async remote = session.getAsyncRemote();
        // 获取Jar，Git需要先 clone成Jar计算MD5，JarFile则直接下载
        ContainerSourceType sourceType = ContainerSourceType.of(container.getSourceType());
        if (sourceType == ContainerSourceType.Git) {

            String workerDirStr = OmsFileUtils.genTemporaryWorkePath();
            File workerDir = new File(workerDirStr);
            FileUtils.forceMkdir(workerDir);

            try {
                // git clone
                remote.sendText("SYSTEM: start to git clone the code repo, using config: " + container.getSourceInfo());
                GitRepoInfo gitRepoInfo = JsonUtils.parseObject(container.getSourceInfo(), GitRepoInfo.class);

                CloneCommand cloneCommand = Git.cloneRepository()
                        .setDirectory(workerDir)
                        .setURI(gitRepoInfo.getRepo())
                        .setBranch(gitRepoInfo.getBranch());
                if (!StringUtils.isEmpty(gitRepoInfo.getUsername())) {
                    CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(gitRepoInfo.getUsername(), gitRepoInfo.getPassword());
                    cloneCommand.setCredentialsProvider(credentialsProvider);
                }
                cloneCommand.call();

                // 获取最新的 commitId 作为版本
                String oldVersion = container.getVersion();
                try (Repository repository = Git.open(workerDir).getRepository()) {
                    Ref head = repository.getRefDatabase().findRef("HEAD");
                    container.setVersion(head.getObjectId().getName());
                }

                if (container.getVersion().equals(oldVersion)) {
                    remote.sendText(String.format("SYSTEM: this commitId(%s) is the same as the last.", oldVersion));
                }else {
                    remote.sendText(String.format("SYSTEM: new version detected, from %s to %s.", oldVersion, container.getVersion()));
                }

                // mvn clean package -DskipTests -U
                remote.sendText("SYSTEM: git clone successfully, star to compile the project.");
                Invoker mvnInvoker = new DefaultInvoker();
                InvocationRequest ivkReq = new DefaultInvocationRequest();
                // -U：强制让Maven检查所有SNAPSHOT依赖更新，确保集成基于最新的状态
                // -e：如果构建出现异常，该参数能让Maven打印完整的stack trace
                // -B：让Maven使用批处理模式构建项目，能够避免一些需要人工参与交互而造成的挂起状态
                ivkReq.setGoals(Lists.newArrayList("clean", "package", "-DskipTests", "-U", "-e", "-B"));
                ivkReq.setBaseDirectory(workerDir);
                ivkReq.setOutputHandler(remote::sendText);
                ivkReq.setBatchMode(true);

                mvnInvoker.execute(ivkReq);

                String targetDirStr = workerDirStr + "/target";
                File targetDir = new File(targetDirStr);
                IOFileFilter fileFilter = FileFilterUtils.asFileFilter((dir, name) -> name.endsWith("jar-with-dependencies.jar"));
                Collection<File> jarFile = FileUtils.listFiles(targetDir, fileFilter, null);

                if (CollectionUtils.isEmpty(jarFile)) {
                    remote.sendText("SYSTEM: can't find packaged jar(maybe maven build failed), so deploy failed.");
                    return null;
                }

                File jarWithDependency = jarFile.iterator().next();

                String jarFileName = genContainerJarName(container.getVersion());

                if (!gridFsManager.exists(GridFsManager.CONTAINER_BUCKET, jarFileName)) {
                    remote.sendText("SYSTEM: can't find the jar resource in remote, maybe this is a new version, start to upload new version.");
                    gridFsManager.download(jarWithDependency, GridFsManager.CONTAINER_BUCKET, jarFileName);
                    remote.sendText("SYSTEM: upload to GridFS successfully~");
                }

                // 将文件从临时工作目录移动到正式目录
                String localFileStr = OmsFileUtils.genContainerJarPath() + jarFileName;
                File localFile = new File(localFileStr);
                if (localFile.exists()) {
                    FileUtils.forceDelete(localFile);
                }
                FileUtils.copyFile(jarWithDependency, localFile);

                return localFile;
            }finally {
                // 删除工作区数据
                FileUtils.forceDelete(workerDir);
            }
        }

        // 先查询本地是否存在目标 Jar 文件
        String jarFileName = genContainerJarName(container.getVersion());
        String localFileStr = OmsFileUtils.genContainerJarPath() + jarFileName;
        File localFile = new File(localFileStr);
        if (localFile.exists()) {
            remote.sendText("SYSTEM: find the jar file in local disk.");
            return localFile;
        }

        // 从 MongoDB 下载
        remote.sendText(String.format("SYSTEM: try to find the jarFile(%s) in GridFS", jarFileName));
        downloadJarFromGridFS(jarFileName, localFile);
        remote.sendText("SYSTEM: download jar file from GridFS successfully~");
        return localFile;
    }

    private void downloadJarFromGridFS(String mongoFileName, File targetFile) {
        synchronized (("dlLock-" + mongoFileName).intern()) {
            if (targetFile.exists()) {
                return;
            }
            if (!gridFsManager.exists(GridFsManager.CONTAINER_BUCKET, mongoFileName)) {
                log.warn("[ContainerService] can't find container's jar file({}) in gridFS.", mongoFileName);
                return;
            }
            try {
                gridFsManager.download(targetFile, GridFsManager.CONTAINER_BUCKET, mongoFileName);
            }catch (Exception e) {
                CommonUtils.executeIgnoreException(() -> FileUtils.forceDelete(targetFile));
                ExceptionUtils.rethrow(e);
            }
        }
    }

    private static String genContainerJarName(String version) {
        return String.format("oms-container-%s.jar", version);
    }

    /**
     * 计算 sleep 时间（每10M睡眠1S + 1）
     * @param fileLength 文件的字节数
     * @return sleep 时间
     */
    private long calculateSleepTime(long fileLength) {
        return (fileLength / FileUtils.ONE_MB / 10 + 1) * 1000;
    }

}
