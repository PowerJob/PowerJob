package tech.powerjob.server.extension.dfs;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 文件路径
 *
 * @author tjq
 * @since 2023/7/16
 */
@Getter
@Setter
@Accessors(chain = true)
public class FileLocation {

    /**
     * 存储桶
     */
    private String bucket;

    /**
     * 名称
     */
    private String name;

    @Override
    public String toString() {
        return String.format("%s.%s", bucket, name);
    }
}
