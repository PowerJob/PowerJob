package tech.powerjob.server.persistence.storage.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tech.powerjob.server.extension.dfs.*;
import tech.powerjob.server.persistence.storage.AbstractDFsService;

import java.io.IOException;
import java.util.Optional;

/**
 * Alibaba OSS support
 * <a href="https://www.aliyun.com/product/oss">海量、安全、低成本、高可靠的云存储服务</a>
 *
 * @author tjq
 * @since 2023/7/30
 */
@Slf4j
@Service
@ConditionalOnProperty(name = {"oms.storage.dfs.alioss.uri"}, matchIfMissing = false)
@ConditionalOnMissingBean(DFsService.class)
public class AliOssService extends AbstractDFsService {

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void store(StoreRequest storeRequest) throws IOException {

    }

    @Override
    public void download(DownloadRequest downloadRequest) throws IOException {

    }

    @Override
    public Optional<FileMeta> fetchFileMeta(FileLocation fileLocation) throws IOException {
        return Optional.empty();
    }
}
