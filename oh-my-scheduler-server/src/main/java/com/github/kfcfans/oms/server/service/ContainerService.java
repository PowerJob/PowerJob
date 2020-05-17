package com.github.kfcfans.oms.server.service;

import akka.actor.ActorSelection;
import com.github.kfcfans.oms.common.model.GitRepoInfo;
import com.github.kfcfans.oms.common.request.ServerDeployContainerRequest;
import com.github.kfcfans.oms.common.utils.CommonUtils;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.common.utils.NetUtils;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.akka.actors.ServerActor;
import com.github.kfcfans.oms.server.common.constans.ContainerSourceType;
import com.github.kfcfans.oms.server.common.utils.OmsFileUtils;
import com.github.kfcfans.oms.server.persistence.core.model.ContainerInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.ContainerInfoRepository;
import com.github.kfcfans.oms.server.service.ha.ClusterStatusHolder;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
import com.github.kfcfans.oms.server.service.lock.LockService;
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
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
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

    private GridFsTemplate gridFsTemplate;

    // 并发部署的机器数量
    private static final int DEPLOY_BATCH_NUM = 50;

    /**
     * 获取构建容器所需要的 Jar 文件
     * @param filename 文件名称
     * @return 本地Jar文件
     */
    public File fetchContainerJarFile(String filename) {

        String jarFileName = OmsFileUtils.genContainerJarPath() + filename;
        File jarFile = new File(jarFileName);

        if (jarFile.exists()) {
            return jarFile;
        }
        if (gridFsTemplate != null) {
            downloadJarFromGridFS(filename, jarFile);
        }
        return jarFile;
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

            // 准备文件
            File jarFile = prepareJarFile(container, session);
            double sizeMB = 1.0 * jarFile.length() / FileUtils.ONE_MB;
            remote.sendText(String.format("SYSTEM: the jarFile(size=%fMB) is prepared and ready to be deployed to the worker.", sizeMB));

            // 开始部署（需要分批进行）
            Set<String> workerAddressList = WorkerManagerService.getActiveWorkerInfo(container.getAppId()).keySet();
            if (workerAddressList.isEmpty()) {
                remote.sendText("SYSTEM: there is no worker available now, deploy failed!");
                return;
            }

            String port = environment.getProperty("local.server.port");
            String downloadURL = String.format("http://%s:%s/container/downloadJar?filename=%s", NetUtils.getLocalHost(), port, jarFile.getName());
            ServerDeployContainerRequest req = new ServerDeployContainerRequest(containerName, container.getMd5(), downloadURL);
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

            String workerDirStr = OmsFileUtils.genTemporaryPath();
            File workerDir = new File(workerDirStr);
            FileUtils.forceMkdir(workerDir);

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

            // mvn clean package -DskipTests -U
            remote.sendText("SYSTEM: git clone successfully, star to compile the project.");
            Invoker mvnInvoker = new DefaultInvoker();
            InvocationRequest ivkReq = new DefaultInvocationRequest();
            ivkReq.setGoals(Lists.newArrayList("clean", "package", "-DskipTests", "-U"));
            ivkReq.setBaseDirectory(workerDir);
            ivkReq.setOutputHandler(remote::sendText);

            InvocationResult mvnResult = mvnInvoker.execute(ivkReq);
            if (mvnResult.getExitCode() != 0) {
                throw mvnResult.getExecutionException();
            }

            String targetDirStr = workerDirStr + "/target";
            File targetDir = new File(targetDirStr);
            IOFileFilter fileFilter = FileFilterUtils.asFileFilter((dir, name) -> name.endsWith("jar-with-dependencies.jar"));
            Collection<File> jarFile = FileUtils.listFiles(targetDir, fileFilter, null);

            if (CollectionUtils.isEmpty(jarFile)) {
                remote.sendText("SYSTEM: can't find packaged jar, deploy failed!");
                throw new RuntimeException("can't find packaged jar");
            }

            File jarWithDependency = jarFile.iterator().next();
            String md5 = OmsFileUtils.md5(jarWithDependency);
            // 更新 MD5
            container.setMd5(md5);

            String jarFileName = genContainerJarName(md5);
            GridFsResource resource = gridFsTemplate.getResource(jarFileName);

            if (!resource.exists()) {
                remote.sendText("SYSTEM: can't find the jar resource in remote, maybe this is a new version, start to upload new version.");
                OmsFileUtils.storeFile2GridFS(gridFsTemplate, jarWithDependency, jarFileName, null);
                remote.sendText("SYSTEM: upload to GridFS successfully~");
            }

            // 将文件从临时工作目录移动到正式目录
            String localFileStr = OmsFileUtils.genContainerJarPath() + jarFileName;
            File localFile = new File(localFileStr);
            FileUtils.forceDelete(localFile);
            FileUtils.copyFile(jarWithDependency, localFile);

            // 删除工作区数据
            FileUtils.forceDelete(workerDir);

            return localFile;
        }

        // 先查询本地是否存在目标 Jar 文件
        String jarFileName = genContainerJarName(container.getMd5());
        String localFileStr = OmsFileUtils.genContainerJarPath() + jarFileName;
        File localFile = new File(localFileStr);
        if (localFile.exists()) {
            remote.sendText("SYSTEM: find the jar file in local disk.");
            return localFile;
        }

        // 从 MongoDB 下载
        GridFsResource resource = gridFsTemplate.getResource(jarFileName);
        if (!resource.exists()) {
            remote.sendText(String.format("SYSTEM: can't find %s in local disk and GridFS, deploy failed!", jarFileName));
            throw new RuntimeException("can't find jar");
        }
        remote.sendText("SYSTEM: start to download jar file from GridFS......");
        OmsFileUtils.gridFs2File(resource, localFile);
        remote.sendText("SYSTEM: download jar file from GridFS successfully~");
        return localFile;
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

    /**
     * 计算 sleep 时间（每10M睡眠1S + 1）
     * @param fileLength 文件的字节数
     * @return sleep 时间
     */
    private long calculateSleepTime(long fileLength) {
        return (fileLength / FileUtils.ONE_MB / 10 + 1) * 1000;
    }
}
