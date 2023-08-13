package tech.powerjob.server.persistence.storage.impl;

import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyun.oss.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.powerjob.server.extension.dfs.DFsService;

import java.util.Optional;


/**
 * test AliOSS
 *
 * @author tjq
 * @since 2023/7/30
 */
@Slf4j
class AliOssServiceTest extends AbstractDfsServiceTest {

    private static final String BUCKET = "power-job";

    /**
     * 依赖阿里云账号密码测试，为了保证单测在其他环境也能通过，如果发现不存在配置则直接跳过
     * @return AliOssService
     */
    @Override
    protected Optional<DFsService> fetchService() {
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