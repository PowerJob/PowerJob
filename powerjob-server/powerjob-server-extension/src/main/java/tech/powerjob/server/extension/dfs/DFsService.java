package tech.powerjob.server.extension.dfs;

import java.io.IOException;
import java.util.Optional;

/**
 * 分布式文件服务
 *
 * @author tjq
 * @since 2023/7/16
 */
public interface DFsService {

    /**
     * 存储文件
     * @param storeRequest 存储请求
     * @throws IOException 异常
     */
    void store(StoreRequest storeRequest) throws IOException;

    /**
     * 下载文件
     * @param downloadRequest 文件下载请求
     * @throws IOException 异常
     */
    void download(DownloadRequest downloadRequest) throws IOException;

    /**
     * 获取文件元信息
     * @param fileLocation 文件位置
     * @return 存在则返回文件元信息
     * @throws IOException 异常
     */
    Optional<FileMeta> fetchFileMeta(FileLocation fileLocation) throws IOException;

    /**
     * 清理 powerjob 认为“过期”的文件
     * 部分存储系统自带生命周期管理（如阿里云OSS，则不需要单独实现该方法）
     * @param bucket bucket
     * @param days 天数，需要清理超过 X 天的文件
     */
    default void cleanExpiredFiles(String bucket, int days) {
    }
}
