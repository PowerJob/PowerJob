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

    private static final Map<String, OmsContainer> CARGO = Maps.newConcurrentMap();

    /**
     * 获取容器
     * @param name 容器名称
     * @return 容器示例，可能为 null
     */
    public static OmsContainer getContainer(String name) {

        OmsContainer omsContainer = CARGO.get(name);
        if (omsContainer != null) {
            return omsContainer;
        }

        // 尝试下载
        WorkerNeedDeployContainerRequest request = new WorkerNeedDeployContainerRequest(name);

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
                deployContainer(deployRequest);
            }
        }catch (Exception e) {
            log.error("[OmsContainer] get container(name={}) failed.", name, e);
        }

        return CARGO.get(name);
    }


    /**
     * 部署容器，整个过程串行进行，问题不大
     * @param request 部署容器请求
     */
    public static synchronized void deployContainer(ServerDeployContainerRequest request) {

        String containerName = request.getContainerName();
        String version = request.getVersion();

        OmsContainer oldContainer = CARGO.get(containerName);
        if (oldContainer != null && version.equals(oldContainer.getVersion())) {
            log.info("[OmsContainerFactory] container(name={},version={}) already deployed.", containerName, version);
            return;
        }

        try {

            // 下载Container到本地
            String filePath = OmsWorkerFileUtils.getContainerDir() + containerName + "/" + version + ".jar";
            File jarFile = new File(filePath);
            if (!jarFile.exists()) {
                FileUtils.forceMkdirParent(jarFile);
                FileUtils.copyURLToFile(new URL(request.getDownloadURL()), jarFile, 5000, 300000);
                log.info("[OmsContainerFactory] download Jar for container({}) successfully.", containerName);
            }

            // 创建新容器
            OmsContainer newContainer = new OmsJarContainer(request.getContainerId(), containerName, version, jarFile);
            newContainer.init();

            // 替换容器
            CARGO.put(containerName, newContainer);
            log.info("[OmsContainerFactory] container(name={},version={}) deployed successfully.", containerName, version);

            if (oldContainer != null) {
                // 销毁旧容器
                oldContainer.destroy();
            }

        }catch (Exception e) {
            log.error("[OmsContainerFactory] deploy container(name={},version={}) failed.", containerName, version, e);
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
