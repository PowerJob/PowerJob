package com.github.kfcfans.oms.worker.container;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.github.kfcfans.oms.common.RemoteConstant;
import com.github.kfcfans.oms.common.model.DeployedContainerInfo;
import com.github.kfcfans.oms.common.request.ServerDeployContainerRequest;
import com.github.kfcfans.oms.common.request.http.WorkerNeedDeployContainerRequest;
import com.github.kfcfans.oms.common.response.AskResponse;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.oms.worker.common.utils.OmsWorkerFileUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * 容器工厂
 *
 * @author tjq
 * @since 2020/5/16
 */
@Slf4j
public class OmsContainerFactory {

    private static final Map<Long, OmsContainer> CARGO = Maps.newConcurrentMap();

    /**
     * 获取容器
     * @param containerId 容器ID
     * @return 容器示例，可能为 null
     */
    public static OmsContainer getContainer(Long containerId) {

        OmsContainer omsContainer = CARGO.get(containerId);
        if (omsContainer != null) {
            return omsContainer;
        }

        // 尝试下载
        log.info("[OmsContainer-{}] can't find the container in factory, try to deploy from server.", containerId);
        WorkerNeedDeployContainerRequest request = new WorkerNeedDeployContainerRequest(containerId);

        String serverPath = AkkaUtils.getAkkaServerPath(RemoteConstant.SERVER_ACTOR_NAME);
        if (StringUtils.isEmpty(serverPath)) {
            return null;
        }
        ActorSelection serverActor = OhMyWorker.actorSystem.actorSelection(serverPath);
        try {

            CompletionStage<Object> askCS = Patterns.ask(serverActor, request, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));
            AskResponse askResponse = (AskResponse) askCS.toCompletableFuture().get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (askResponse.isSuccess()) {
                ServerDeployContainerRequest deployRequest = askResponse.getData(ServerDeployContainerRequest.class);
                log.info("[OmsContainer-{}] fetch containerInfo from server successfully.", containerId);
                deployContainer(deployRequest);
            }
        }catch (Exception e) {
            log.error("[OmsContainer-{}] deployed container failed, exception is {}", containerId, e.toString());
        }

        return CARGO.get(containerId);
    }


    /**
     * 部署容器，整个过程串行进行，问题不大
     * @param request 部署容器请求
     */
    public static synchronized void deployContainer(ServerDeployContainerRequest request) {

        Long containerId = request.getContainerId();
        String containerName = request.getContainerName();
        String version = request.getVersion();

        log.info("[OmsContainer-{}] start to deploy container(name={},version={},downloadUrl={})", containerId, containerName, version, request.getDownloadURL());

        OmsContainer oldContainer = CARGO.get(containerId);
        if (oldContainer != null && version.equals(oldContainer.getVersion())) {
            log.info("[OmsContainer-{}] version={} already deployed, so skip this deploy task.", containerId, version);
            return;
        }

        try {

            // 下载Container到本地
            String filePath = OmsWorkerFileUtils.getContainerDir() + containerId + "/" + version + ".jar";
            File jarFile = new File(filePath);
            if (!jarFile.exists()) {
                FileUtils.forceMkdirParent(jarFile);
                FileUtils.copyURLToFile(new URL(request.getDownloadURL()), jarFile, 5000, 300000);
                log.info("[OmsContainer-{}] download jar successfully, path={}", containerId, jarFile.getPath());
            }

            // 创建新容器
            OmsContainer newContainer = new OmsJarContainer(containerId, containerName, version, jarFile);
            newContainer.init();

            // 替换容器
            CARGO.put(containerId, newContainer);
            log.info("[OmsContainer-{}] deployed new version:{} successfully!", containerId, version);

            if (oldContainer != null) {
                // 销毁旧容器
                oldContainer.destroy();
            }

        }catch (Exception e) {
            log.error("[OmsContainer-{}] deployContainer(name={},version={}) failed.", containerId, containerName, version, e);
        }
    }

    /**
     * 获取该Worker已部署容器的信息
     * @return 已部署容器信息
     */
    public static List<DeployedContainerInfo> getDeployedContainerInfos() {
        List<DeployedContainerInfo> info = Lists.newLinkedList();
        CARGO.forEach((name, container) -> info.add(new DeployedContainerInfo(container.getContainerId(), container.getVersion(), container.getDeployedTime(), null)));
        return info;
    }
}
