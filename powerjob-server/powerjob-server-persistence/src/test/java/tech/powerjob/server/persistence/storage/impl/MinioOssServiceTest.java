package tech.powerjob.server.persistence.storage.impl;

import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.powerjob.server.extension.dfs.DFsService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * desc
 *
 * @author tjq
 * @since 2024/2/26
 */
class MinioOssServiceTest extends AbstractDfsServiceTest {

    @Override
    protected Optional<DFsService> fetchService() {
        try {
            MinioOssService aliOssService = new MinioOssService();
            aliOssService.initOssClient("http://192.168.124.23:9000", "pj2","testAk", "testSktestSktestSk");
            return Optional.of(aliOssService);
        } catch (Exception e) {
            ExceptionUtils.rethrow(e);
        }
        return Optional.empty();
    }
}