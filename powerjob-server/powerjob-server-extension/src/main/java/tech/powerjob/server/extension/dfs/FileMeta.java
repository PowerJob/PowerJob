package tech.powerjob.server.extension.dfs;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * FileMeta
 *
 * @author tjq
 * @since 2023/7/16
 */
@Data
@Accessors(chain = true)
public class FileMeta {

    /**
     * 文件大小
     */
    private long length;

    /**
     * 元数据
     */
    private Map<String, Object> metaInfo;
}
