package tech.powerjob.server.persistence.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import tech.powerjob.server.extension.dfs.DFsService;
import tech.powerjob.server.persistence.storage.impl.*;

/**
 * 初始化内置的存储服务
 *
 * @author tjq
 * @since 2023/7/30
 */
@Configuration
public class StorageConfiguration {

    @Bean
    @Conditional(GridFsService.GridFsCondition.class)
    public DFsService initGridFs() {
        return new GridFsService();
    }

    @Bean
    @Conditional(MySqlSeriesDfsService.MySqlSeriesCondition.class)
    public DFsService initDbFs() {
        return new MySqlSeriesDfsService();
    }

    @Bean
    @Conditional(AliOssService.AliOssCondition.class)
    public DFsService initAliOssFs() {
        return new AliOssService();
    }

    @Bean
    @Conditional(MinioOssService.MinioOssCondition.class)
    public DFsService initMinioOssFs() {
        return new MinioOssService();
    }

    @Bean
    @Conditional(EmptyDFsService.EmptyCondition.class)
    public DFsService initEmptyDfs() {
        return new EmptyDFsService();
    }
}
