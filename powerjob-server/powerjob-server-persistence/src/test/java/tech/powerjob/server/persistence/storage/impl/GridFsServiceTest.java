package tech.powerjob.server.persistence.storage.impl;

import lombok.extern.slf4j.Slf4j;
import tech.powerjob.server.common.utils.TestUtils;
import tech.powerjob.server.extension.dfs.DFsService;

import java.util.Optional;

/**
 * test GridFS
 *
 * @author tjq
 * @since 2023/7/30
 */
@Slf4j
class GridFsServiceTest extends AbstractDfsServiceTest {

    @Override
    protected Optional<DFsService> fetchService() {

        Object mongoUri = TestUtils.fetchTestConfig().get(TestUtils.KEY_MONGO_URI);

        if (mongoUri == null) {
            log.info("[GridFsServiceTest] mongoUri is null, skip load!");
            return Optional.empty();
        }

        GridFsService gridFsService = new GridFsService();
        gridFsService.initMongo(String.valueOf(mongoUri));
        return Optional.of(gridFsService);
    }
}