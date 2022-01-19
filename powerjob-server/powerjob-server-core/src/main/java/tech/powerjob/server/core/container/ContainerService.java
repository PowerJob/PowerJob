package tech.powerjob.server.core.container;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.persistence.remote.model.ContainerInfoDO;

import javax.websocket.Session;
import java.io.File;
import java.io.IOException;

/**
 * 容器服务
 *
 * @author tjq
 * @since 2020/5/16
 * @deprecated 禁用容器功能
 */
@Slf4j
@Service
@Deprecated
public class ContainerService {

    private static final String ERROR_INFO = "This feature has been disabled！";

    /**
     * 保存容器
     *
     * @param container 容器保存请求
     */
    public void save(ContainerInfoDO container) {
        throw new PowerJobException(ERROR_INFO);
    }

    /**
     * 删除容器（通知 Worker 销毁容器 & 删除数据库）
     *
     * @param appId       应用ID，用于保护性判断
     * @param containerId 容器ID
     */
    public void delete(Long appId, Long containerId) {
        throw new PowerJobException(ERROR_INFO);
    }

    /**
     * 上传用于部署的容器的 Jar 文件
     *
     * @param file 接受的文件
     * @return 该文件的 md5 值
     */
    public String uploadContainerJarFile(MultipartFile file) {
        throw new PowerJobException(ERROR_INFO);
    }

    /**
     * 获取构建容器所需要的 Jar 文件
     *
     * @param version 版本
     * @return 本地Jar文件
     */
    public File fetchContainerJarFile(String version) {
        throw new PowerJobException(ERROR_INFO);
    }

    /**
     * 部署容器
     *
     * @param containerId 容器ID
     * @param session     WebSocket Session
     */
    public void deploy(Long containerId, Session session) {
        throw new PowerJobException(ERROR_INFO);
    }

    /**
     * 获取部署信息
     *
     * @param appId       容器所属应用ID
     * @param containerId 容器ID
     * @return 拼接好的可阅读字符串
     */
    public String fetchDeployedInfo(Long appId, Long containerId) {
        throw new PowerJobException(ERROR_INFO);
    }


}
