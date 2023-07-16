package tech.powerjob.server.extension.dfs;

import lombok.Data;

import java.util.Map;
import java.util.Objects;

/**
 * FileMeta
 *
 * @author tjq
 * @since 2023/7/16
 */
@Data
public class FileMeta {

    /**
     * 文件大小
     */
    private final long length;

    /**
     * 元数据
     */
    private Map<String, Objects> metaInfo;
}
