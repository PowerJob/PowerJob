package tech.powerjob.server.persistence.storage.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import tech.powerjob.server.extension.dfs.*;
import tech.powerjob.server.persistence.storage.AbstractDFsService;
import tech.powerjob.server.common.spring.condition.PropertyAndOneBeanCondition;

import javax.annotation.Priority;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * EmptyDFsService
 *
 * @author tjq
 * @since 2023/7/30
 */
@Slf4j
@Priority(value = Integer.MAX_VALUE)
@Conditional(EmptyDFsService.EmptyCondition.class)
public class EmptyDFsService extends AbstractDFsService {


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

    @Override
    public void destroy() throws Exception {
    }

    @Override
    protected void init(ApplicationContext applicationContext) {
        log.info("[EmptyDFsService] initialize successfully, THIS_WILL_BE_THE_STORAGE_LAYER.");
    }


    public static class EmptyCondition extends PropertyAndOneBeanCondition {
        @Override
        protected List<String> anyConfigKey() {
            return null;
        }

        @Override
        protected Class<?> beanType() {
            return DFsService.class;
        }
    }
}
