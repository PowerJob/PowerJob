package tech.powerjob.server.persistence.storage.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.common.utils.OmsFileUtils;
import tech.powerjob.server.extension.dfs.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * AbstractDfsServiceTest
 *
 * @author tjq
 * @since 2023/7/30
 */
@Slf4j
public abstract class AbstractDfsServiceTest {

    abstract protected Optional<DFsService> fetchService();

    @Test
    void testBaseFileOperation() throws Exception {

        Optional<DFsService> aliOssServiceOpt = fetchService();
        if (!aliOssServiceOpt.isPresent()) {
            return;
        }

        DFsService aliOssService = aliOssServiceOpt.get();

        String content = "wlcgyqsl";

        String temporarySourcePath = OmsFileUtils.genTemporaryWorkPath() + "source.txt";
        String temporaryDownloadPath = OmsFileUtils.genTemporaryWorkPath() + "download.txt";

        log.info("[testBaseFileOperation] temporarySourcePath: {}", temporarySourcePath);
        File sourceFile = new File(temporarySourcePath);
        FileUtils.forceMkdirParent(sourceFile);
        OmsFileUtils.string2File(content, sourceFile);

        FileLocation fileLocation = new FileLocation().setBucket("pj_test").setName("testAliOss.txt");

        StoreRequest storeRequest = new StoreRequest()
                .setFileLocation(fileLocation)
                .setLocalFile(sourceFile);

        // 存储
        aliOssService.store(storeRequest);

        // 读取 meta
        Optional<FileMeta> metaOpt = aliOssService.fetchFileMeta(fileLocation);
        assert metaOpt.isPresent();

        log.info("[testBaseFileOperation] file meta: {}", JsonUtils.toJSONString(metaOpt.get()));

        // 下载
        log.info("[testBaseFileOperation] temporaryDownloadPath: {}", temporaryDownloadPath);
        File downloadFile = new File(temporaryDownloadPath);
        DownloadRequest downloadRequest = new DownloadRequest()
                .setFileLocation(fileLocation)
                .setTarget(downloadFile);
        aliOssService.download(downloadRequest);

        String downloadFileContent = FileUtils.readFileToString(downloadFile, StandardCharsets.UTF_8);
        log.info("[testBaseFileOperation] download content: {}", downloadFileContent);
        assert downloadFileContent.equals(content);
    }

    @Test
    void testFileNotExist() throws Exception {
        Optional<DFsService> aliOssServiceOpt = fetchService();
        if (!aliOssServiceOpt.isPresent()) {
            return;
        }
        Optional<FileMeta> metaOpt = aliOssServiceOpt.get().fetchFileMeta(new FileLocation().setBucket("tjq").setName("yhz"));
        assert !metaOpt.isPresent();
    }
}
