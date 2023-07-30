package tech.powerjob.server.persistence.storage.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import tech.powerjob.server.extension.dfs.*;

import java.io.IOException;
import java.util.Optional;

/**
 * EmptyDFsService
 *
 * @author tjq
 * @since 2023/7/30
 */
@Service
@Order(value = Ordered.LOWEST_PRECEDENCE)
@ConditionalOnMissingBean(DFsService.class)
public class EmptyDFsService implements DFsService {

    public EmptyDFsService() {
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
