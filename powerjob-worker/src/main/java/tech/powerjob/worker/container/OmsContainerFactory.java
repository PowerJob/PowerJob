package tech.powerjob.worker.container;

import tech.powerjob.common.model.DeployedContainerInfo;
import tech.powerjob.common.request.ServerDeployContainerRequest;
import tech.powerjob.common.request.WorkerNeedDeployContainerRequest;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.common.utils.CommonUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.utils.PowerFileUtils;
import tech.powerjob.worker.common.utils.TransportUtils;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * 容器工厂
 *
 * @author tjq
 * @since 2020/5/16
 */
@Slf4j
public class OmsContainerFactory {

    private static final String CONTAINER_DIR = PowerFileUtils.workspace() + "/container/";
    private static final Map<Long, OmsContainer> CARGO = Maps.newConcurrentMap();

    /**
     * 获取容器
     * @param containerId 容器ID
     * @param workerRuntime 当容器不存在且 serverActor 非空时，尝试从服务端重新拉取容器
     * @return 容器示例，可能为 null
     */
    public static OmsContainer fetchContainer(Long containerId, WorkerRuntime workerRuntime) {

        OmsContainer omsContainer = CARGO.get(containerId);
        if (omsContainer != null) {
            return omsContainer;
        }

        final String currentServerAddress = workerRuntime.getServerDiscoveryService().getCurrentServerAddress();

        // 尝试从 server 加载
        log.info("[OmsContainer-{}] can't find the container in factory, try to deploy from server.", containerId);
        WorkerNeedDeployContainerRequest request = new WorkerNeedDeployContainerRequest(containerId);

        try {

            AskResponse askResponse = TransportUtils.reliableQueryContainerInfo(request, currentServerAddress, workerRuntime.getTransporter());

            if (askResponse.isSuccess()) {
                ServerDeployContainerRequest deployRequest = askResponse.getData(ServerDeployContainerRequest.class);
                log.info("[OmsContainer-{}] fetch containerInfo from server successfully.", containerId);
                deployContainer(deployRequest);
            }else {
                log.warn("[OmsContainer-{}] fetch containerInfo failed, reason is {}.", containerId, askResponse.getMessage());
            }
        }catch (Exception e) {
            log.error("[OmsContainer-{}] get container failed, exception is {}", containerId, e.toString());
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

        String filePath = CONTAINER_DIR + containerId + "/" + version + ".jar";
        // 下载Container到本地
        File jarFile = new File(filePath);

        try {
            if (!jarFile.exists()) {
                log.info("[OmsContainer-{}] container not exist(path={}), try to download from server!", containerId, jarFile.getPath());
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
                log.info("[OmsContainer-{}] start to destroy old container(version={})", containerId, oldContainer.getVersion());
                oldContainer.destroy();
            }

        } catch (Exception e) {
            log.error("[OmsContainer-{}] deployContainer(name={},version={}) failed.", containerId, containerName, version, e);
            // 如果部署失败，则删除该 jar（本次失败可能是下载jar出错导致，不删除会导致这个版本永久无法重新部署）
            CommonUtils.executeIgnoreException(() -> FileUtils.forceDelete(jarFile));
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

    /**
     * 销毁指定容器
     * @param containerId 容器ID
     */
    public static void destroyContainer(Long containerId) {
        OmsContainer container = CARGO.remove(containerId);
        if (container == null) {
            log.info("[OmsContainer-{}] container not exists, so there is no need to destroy the container.", containerId);
            return;
        }
        try {
            container.destroy();
        }catch (Exception e) {
            log.warn("[OmsContainer-{}] destroy container failed.", containerId, e);
        }
    }
}
