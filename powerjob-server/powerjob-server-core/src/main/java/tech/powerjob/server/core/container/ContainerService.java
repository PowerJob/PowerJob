package tech.powerjob.server.core.container;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.exception.ImpossibleException;
import tech.powerjob.common.model.DeployedContainerInfo;
import tech.powerjob.common.model.GitRepoInfo;
import tech.powerjob.common.request.ServerDeployContainerRequest;
import tech.powerjob.common.request.ServerDestroyContainerRequest;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.common.utils.SegmentLock;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.server.common.constants.ContainerSourceType;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.common.module.WorkerInfo;
import tech.powerjob.server.common.utils.OmsFileUtils;
import tech.powerjob.server.extension.LockService;
import tech.powerjob.server.extension.dfs.*;
import tech.powerjob.server.persistence.remote.model.ContainerInfoDO;
import tech.powerjob.server.persistence.remote.repository.ContainerInfoRepository;
import tech.powerjob.server.persistence.storage.Constants;
import tech.powerjob.server.remote.server.redirector.DesignateServer;
import tech.powerjob.server.remote.transporter.impl.ServerURLFactory;
import tech.powerjob.server.remote.transporter.TransportService;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;

import javax.annotation.Resource;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    private DFsService dFsService;
    @Resource
    private TransportService transportService;

    @Resource
    private WorkerClusterQueryService workerClusterQueryService;

    // 下载用的分段锁
    private final SegmentLock segmentLock = new SegmentLock(4);
    // 并发部署的机器数量
    private static final int DEPLOY_BATCH_NUM = 50;
    // 部署间隔
    private static final long DEPLOY_MIN_INTERVAL = 10 * 60 * 1000L;
    // 最长部署时间
    private static final long DEPLOY_MAX_COST_TIME = 10 * 60 * 1000L;

    /**
     * 保存容器
     * @param container 容器保存请求
     */
    public void save(ContainerInfoDO container) {


        Long originId = container.getId();
        if (originId != null) {
            // just validate
            containerInfoRepository.findById(originId).orElseThrow(() -> new IllegalArgumentException("can't find container by id: " + originId));
        } else {
            container.setGmtCreate(new Date());
        }
        container.setGmtModified(new Date());

        // 文件上传形式的 sourceInfo 为该文件的 md5 值，Git形式的 md5 在部署阶段生成
        if (container.getSourceType() == ContainerSourceType.FatJar.getV()) {
            container.setVersion(container.getSourceInfo());
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

        ServerDestroyContainerRequest destroyRequest = new ServerDestroyContainerRequest(container.getId());
        workerClusterQueryService.getAllAliveWorkers(container.getAppId()).forEach(workerInfo -> {
            final URL url = ServerURLFactory.destroyContainer2Worker(workerInfo.getAddress());
            transportService.tell(workerInfo.getProtocol(), url, destroyRequest);
        });

        log.info("[ContainerService] delete container: {}.", container);
        // 软删除
        container.setStatus(SwitchableStatus.DELETED.getV());
        container.setGmtModified(new Date());
        containerInfoRepository.saveAndFlush(container);
    }

    /**
     * 上传用于部署的容器的 Jar 文件
     * @param file 接受的文件
     * @return 该文件的 md5 值
     * @throws IOException 异常
     */
    public String uploadContainerJarFile(MultipartFile file) throws IOException {

        log.info("[ContainerService] start to uploadContainerJarFile, fileName={},size={}", file.getName(), file.getSize());

        String workerDirStr = OmsFileUtils.genTemporaryWorkPath();
        String tmpFileStr = workerDirStr + "tmp.jar";

        File workerDir = new File(workerDirStr);
        File tmpFile = new File(tmpFileStr);

        try {
            // 下载到本地
            FileUtils.forceMkdirParent(tmpFile);
            file.transferTo(tmpFile);

            // 生成MD5，这兄弟耗时有点小严重
            String md5 = OmsFileUtils.md5(tmpFile);
            String fileName = genContainerJarName(md5);

            // 上传到 DFS，这兄弟耗时也有点小严重，导致这个接口整体比较慢...不过也没必要开线程去处理
            FileLocation fl = new FileLocation().setBucket(Constants.CONTAINER_BUCKET).setName(fileName);
            StoreRequest storeRequest = new StoreRequest().setLocalFile(tmpFile).setFileLocation(fl);
            dFsService.store(storeRequest);

            // 将文件拷贝到正确的路径
            String finalFileStr = OmsFileUtils.genContainerJarPath() + fileName;
            File finalFile = new File(finalFileStr);
            if (finalFile.exists()) {
                FileUtils.forceDelete(finalFile);
            }
            FileUtils.moveFile(tmpFile, finalFile);

            log.info("[ContainerService] uploadContainerJarFile successfully,md5={}", md5);
            return md5;

        } catch (Throwable t) {
            log.error("[ContainerService] uploadContainerJarFile failed!", t);
            ExceptionUtils.rethrow(t);
            throw new ImpossibleException();
        } finally {
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

        FileLocation fileLocation = new FileLocation().setBucket(Constants.CONTAINER_BUCKET).setName(fileName);
        try {
            Optional<FileMeta> fileMetaOpt = dFsService.fetchFileMeta(fileLocation);
            if (fileMetaOpt.isPresent()) {
                dFsService.download(new DownloadRequest().setFileLocation(fileLocation).setTarget(localFile));
            }
        } catch (Exception e) {
            log.warn("[ContainerService] fetchContainerJarFile from dsf failed, version: {}", version, e);
        }

        return localFile;
    }

    /**
     * 部署容器
     * @param containerId 容器ID
     * @param session WebSocket Session
     * @throws Exception 异常
     */
    public void deploy(Long containerId, Session session) throws Exception {

        String deployLock = "containerDeployLock-" + containerId;
        RemoteEndpoint.Async remote = session.getAsyncRemote();
        // 最长部署时间：10分钟
        boolean lock = lockService.tryLock(deployLock, DEPLOY_MAX_COST_TIME);
        if (!lock) {
            remote.sendText("SYSTEM: acquire deploy lock failed, maybe other user is deploying, please wait until the running deploy task finished.");
            return;
        }

        try {
            Optional<ContainerInfoDO> containerInfoOpt = containerInfoRepository.findById(containerId);
            if (!containerInfoOpt.isPresent()) {
                remote.sendText("SYSTEM: can't find container by id: " + containerId);
                return;
            }
            ContainerInfoDO container = containerInfoOpt.get();

            Date lastDeployTime = container.getLastDeployTime();
            if (lastDeployTime != null) {
                if ((System.currentTimeMillis() - lastDeployTime.getTime()) < DEPLOY_MIN_INTERVAL) {
                    remote.sendText("SYSTEM: [warn] deploy too frequent, last deploy time is: " + DateFormatUtils.format(lastDeployTime, OmsConstant.TIME_PATTERN));
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
            remote.sendText(String.format("SYSTEM: update current container version=%s successfully!", container.getVersion()));

            // 开始部署（需要分批进行）
            final List<WorkerInfo> allAliveWorkers = workerClusterQueryService.getAllAliveWorkers(container.getAppId());
            if (allAliveWorkers.isEmpty()) {
                remote.sendText("SYSTEM: there is no worker available now, deploy failed!");
                return;
            }

            String port = environment.getProperty("local.server.port");
            String downloadURL = String.format("http://%s:%s/container/downloadJar?version=%s", NetUtils.getLocalHost(), port, container.getVersion());
            ServerDeployContainerRequest req = new ServerDeployContainerRequest(containerId, container.getContainerName(), container.getVersion(), downloadURL);
            long sleepTime = calculateSleepTime(jarFile.length());

            AtomicInteger count = new AtomicInteger();
            allAliveWorkers.forEach(workerInfo -> {

                final URL url = ServerURLFactory.deployContainer2Worker(workerInfo.getAddress());
                transportService.tell(workerInfo.getProtocol(), url, req);

                remote.sendText("SYSTEM: send deploy request to " + url.getAddress());

                if (count.incrementAndGet() % DEPLOY_BATCH_NUM == 0) {
                    CommonUtils.executeIgnoreException(() -> Thread.sleep(sleepTime));
                }
            });

            remote.sendText("SYSTEM: deploy finished, congratulations!");

        }finally {
            lockService.unlock(deployLock);
        }
    }

    /**
     * 获取部署信息
     * @param appId 容器所属应用ID
     * @param containerId 容器ID
     * @return 拼接好的可阅读字符串
     */
    @DesignateServer
    public String fetchDeployedInfo(Long appId, Long containerId) {
        List<DeployedContainerInfo> infoList = workerClusterQueryService.getDeployedContainerInfos(appId, containerId);

        Set<String> aliveWorkers = workerClusterQueryService.getAllAliveWorkers(appId)
                .stream()
                .map(WorkerInfo::getAddress)
                .collect(Collectors.toSet());

        Set<String> deployedList = Sets.newLinkedHashSet();
        List<String> unDeployedList = Lists.newLinkedList();
        Multimap<String, String> version2Address = ArrayListMultimap.create();
        infoList.forEach(info -> {
            String targetWorkerAddress = info.getWorkerAddress();
            if (aliveWorkers.contains(targetWorkerAddress)) {
                deployedList.add(targetWorkerAddress);
                version2Address.put(info.getVersion(), targetWorkerAddress);
            }else {
                unDeployedList.add(targetWorkerAddress);
            }
        });

        StringBuilder sb = new StringBuilder("========== DeployedInfo ==========").append(System.lineSeparator());
        // 集群分裂，各worker版本不统一，问题很大
        if (version2Address.keySet().size() > 1) {
            sb.append("WARN: there exists multi version container now, please redeploy to fix this problem").append(System.lineSeparator());
            sb.append("divisive version ==> ").append(System.lineSeparator());
            version2Address.forEach((v, addressList) -> {
                sb.append("version: ").append(v).append(System.lineSeparator());
                sb.append(addressList);
            });
            sb.append(System.lineSeparator());
        }
        // 当前在线未部署机器
        if (!CollectionUtils.isEmpty(unDeployedList)) {
            sb.append("WARN: there exists unDeployed worker(OhMyScheduler will auto fix when some job need to process)").append(System.lineSeparator());
            sb.append("unDeployed worker list ==> ").append(System.lineSeparator());
        }
        // 当前部署机器
        sb.append("deployed worker list ==> ").append(System.lineSeparator());
        if (CollectionUtils.isEmpty(deployedList)) {
            sb.append("no worker deployed now~");
        }else {
            sb.append(deployedList);
        }

        return sb.toString();
    }

    private File prepareJarFile(ContainerInfoDO container, Session session) throws Exception {

        RemoteEndpoint.Async remote = session.getAsyncRemote();
        // 获取Jar，Git需要先 clone成Jar计算MD5，JarFile则直接下载
        ContainerSourceType sourceType = ContainerSourceType.of(container.getSourceType());
        if (sourceType == ContainerSourceType.Git) {

            String workerDirStr = OmsFileUtils.genTemporaryWorkPath();
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
                remote.sendText("SYSTEM: git clone successfully, star to compile the project.");

                // mvn clean package -DskipTests -U
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

                FileLocation dfsFL = new FileLocation().setBucket(Constants.CONTAINER_BUCKET).setName(jarFileName);
                Optional<FileMeta> dfsMetaOpt = dFsService.fetchFileMeta(dfsFL);
                if (dfsMetaOpt.isPresent()) {
                    remote.sendText("SYSTEM: find the jar resource in remote successfully, so it's no need to upload anymore.");
                } else {
                    remote.sendText("SYSTEM: can't find the jar resource in remote, maybe this is a new version, start to upload new version.");
                    dFsService.store(new StoreRequest().setFileLocation(dfsFL).setLocalFile(jarWithDependency));
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
            } catch (Throwable  t) {
                log.error("[ContainerService] prepareJarFile failed for container: {}", container, t);
                remote.sendText("SYSTEM: [ERROR] prepare jar file failed: " + ExceptionUtils.getStackTrace(t));
            } finally {
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

        int lockId = mongoFileName.hashCode();
        try {
            segmentLock.lockInterruptibleSafe(lockId);

            if (targetFile.exists()) {
                return;
            }

            try {

                FileLocation dfsFL = new FileLocation().setBucket(Constants.CONTAINER_BUCKET).setName(mongoFileName);
                Optional<FileMeta> dfsMetaOpt = dFsService.fetchFileMeta(dfsFL);
                if (!dfsMetaOpt.isPresent()) {
                    log.warn("[ContainerService] can't find container's jar file({}) in gridFS.", mongoFileName);
                    return;
                }

                FileUtils.forceMkdirParent(targetFile);

                dFsService.download(new DownloadRequest().setTarget(targetFile).setFileLocation(dfsFL));
            }catch (Exception e) {
                CommonUtils.executeIgnoreException(() -> FileUtils.forceDelete(targetFile));
                ExceptionUtils.rethrow(e);
            }

        }finally {
            segmentLock.unlock(lockId);
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
