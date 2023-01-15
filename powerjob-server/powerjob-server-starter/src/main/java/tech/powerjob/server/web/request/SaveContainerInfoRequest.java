package tech.powerjob.server.web.request;

import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.common.constants.ContainerSourceType;
import tech.powerjob.server.common.constants.SwitchableStatus;
import lombok.Data;

/**
 * 保存/修改 容器 请求
 *
 * @author tjq
 * @since 2020/5/15
 */
@Data
public class SaveContainerInfoRequest {

    /**
     * 容器ID，null -> 创建；否则代表修改
     */
    private Long id;

    /**
     * 所属的应用ID
     */
    private Long appId;

    /**
     * 容器名称
     */
    private String containerName;

    /**
     * 容器类型，枚举值为 ContainerSourceType（JarFile/Git）
     */
    private ContainerSourceType sourceType;
    /**
     * 由 sourceType 决定，JarFile -> String，存储文件名称；Git -> JSON，包括 URL，branch，username，password
     */
    private String sourceInfo;

    /**
     * 状态，枚举值为 ContainerStatus（ENABLE/DISABLE）
     */
    private SwitchableStatus status;

    public void valid() {
        CommonUtils.requireNonNull(containerName, "containerName can't be empty");
        CommonUtils.requireNonNull(appId, "appId can't be empty");
        CommonUtils.requireNonNull(sourceInfo, "sourceInfo can't be empty");
    }
}
