package tech.powerjob.server.persistence.storage.impl;

import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyun.oss.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.common.utils.OmsFileUtils;
import tech.powerjob.server.extension.dfs.DownloadRequest;
import tech.powerjob.server.extension.dfs.FileLocation;
import tech.powerjob.server.extension.dfs.FileMeta;
import tech.powerjob.server.extension.dfs.StoreRequest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


/**
 * test AliOSS
 *
 * @author tjq
 * @since 2023/7/30
 */
@Slf4j
class AliOssServiceTest {

    private static final String BUCKET = "power-job";

    @Test
    void testBaseFileOperation() throws Exception {

        Optional<AliOssService> aliOssServiceOpt = fetchService();
        if (!aliOssServiceOpt.isPresent()) {
            return;
        }

        AliOssService aliOssService = aliOssServiceOpt.get();

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
        Optional<AliOssService> aliOssServiceOpt = fetchService();
        if (!aliOssServiceOpt.isPresent()) {
            return;
        }
        Optional<FileMeta> metaOpt = aliOssServiceOpt.get().fetchFileMeta(new FileLocation().setBucket("tjq").setName("yhz"));
        assert !metaOpt.isPresent();
    }

    /**
     * 依赖阿里云账号密码测试，为了保证单测在其他环境也能通过，如果发现不存在配置则直接跳过
     * @return AliOssService
     */
    private Optional<AliOssService> fetchService() {

        String accessKeyId = StringUtils.trim(System.getenv(AuthUtils.ACCESS_KEY_ENV_VAR));
        String secretAccessKey = StringUtils.trim(System.getenv(AuthUtils.SECRET_KEY_ENV_VAR));

        String bucket = Optional.ofNullable(System.getenv("POWERJOB_OSS_BUEKCT")).orElse(BUCKET);

        log.info("[AliOssServiceTest] ak: {}, sk: {}", accessKeyId, secretAccessKey);

        if (org.apache.commons.lang3.StringUtils.isAnyEmpty(accessKeyId, secretAccessKey)) {
            return Optional.empty();
        }

        try {
            AliOssService aliOssService = new AliOssService();
            aliOssService.initOssClient("oss-cn-beijing.aliyuncs.com", bucket, AliOssService.CredentialType.ENV.getCode(), null, null, null);
            return Optional.of(aliOssService);
        } catch (Exception e) {
            ExceptionUtils.rethrow(e);
        }
        return Optional.empty();
    }
}