package tech.powerjob.server.extension.dfs;

import lombok.Data;

/**
 * 文件路径
 *
 * @author tjq
 * @since 2023/7/16
 */
@Data
public class FileLocation {

    /**
     * 存储桶
     */
    private String bucket;

    /**
     * 名称
     */
    private String name;
}
