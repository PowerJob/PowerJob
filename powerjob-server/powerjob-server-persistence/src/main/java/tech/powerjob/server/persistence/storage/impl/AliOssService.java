package tech.powerjob.server.persistence.storage.impl;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.DownloadFileRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tech.powerjob.server.extension.dfs.*;
import tech.powerjob.server.persistence.storage.AbstractDFsService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Alibaba OSS support
 * <a href="https://www.aliyun.com/product/oss">海量、安全、低成本、高可靠的云存储服务</a>
 * 配置项：
 *  oms.storage.dfs.alioss.endpoint
 *  oms.storage.dfs.alioss.credential_type
 *  oms.storage.dfs.alioss.ak
 *  oms.storage.dfs.alioss.sk
 *  oms.storage.dfs.alioss.token
 *
 * @author tjq
 * @since 2023/7/30
 */
@Slf4j
@Service
@ConditionalOnProperty(name = {"oms.storage.dfs.alioss.endpoint"}, matchIfMissing = false)
@ConditionalOnMissingBean(DFsService.class)
public class AliOssService extends AbstractDFsService {

    private static final String TYPE_ALI_OSS = "alioss";

    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_CREDENTIAL_TYPE = "credential_type";
    private static final String KEY_AK = "ak";
    private static final String KEY_SK = "sk";
    private static final String KEY_TOKEN = "token";

    private OSS oss;

    private static final int DOWNLOAD_PART_SIZE = 10240;

    @Override
    public void afterPropertiesSet() throws Exception {

        String endpoint = fetchProperty(TYPE_ALI_OSS, KEY_ENDPOINT);
        String ct = fetchProperty(TYPE_ALI_OSS, KEY_CREDENTIAL_TYPE);
        String ak = fetchProperty(TYPE_ALI_OSS, KEY_AK);
        String sk = fetchProperty(TYPE_ALI_OSS, KEY_SK);
        String token = fetchProperty(TYPE_ALI_OSS, KEY_TOKEN);

        initOssClient(endpoint, ct, ak, sk, token);
    }

    @Override
    public void store(StoreRequest storeRequest) throws IOException {

        FileLocation dfl = storeRequest.getFileLocation();
        ObjectMetadata objectMetadata = new ObjectMetadata();

        PutObjectRequest putObjectRequest = new PutObjectRequest(dfl.getBucket(), dfl.getName(), storeRequest.getLocalFile(), objectMetadata);
        oss.putObject(putObjectRequest);
    }

    @Override
    public void download(DownloadRequest downloadRequest) throws IOException {

        FileLocation dfl = downloadRequest.getFileLocation();
        DownloadFileRequest downloadFileRequest = new DownloadFileRequest(dfl.getBucket(), dfl.getName(), downloadRequest.getTarget().getAbsolutePath(), DOWNLOAD_PART_SIZE);
        try {
            oss.downloadFile(downloadFileRequest);
        } catch (Throwable t) {
            ExceptionUtils.rethrow(t);
        }
    }

    @Override
    public Optional<FileMeta> fetchFileMeta(FileLocation fileLocation) throws IOException {
        try {
            ObjectMetadata objectMetadata = oss.getObjectMetadata(fileLocation.getBucket(), fileLocation.getName());
            return Optional.ofNullable(objectMetadata).map(ossM -> {

                Map<String, Object> metaInfo = Maps.newHashMap();
                metaInfo.putAll(ossM.getRawMetadata());
                if (ossM.getUserMetadata() != null) {
                    metaInfo.putAll(ossM.getUserMetadata());
                }

                return new FileMeta()
                        .setLastModifiedTime(ossM.getLastModified())
                        .setLength(ossM.getContentLength())
                        .setMetaInfo(metaInfo);
            });
        } catch (OSSException oe) {
            // TODO: 判断文件不存在
        }
        return Optional.empty();
    }

    void initOssClient(String endpoint, String mode, String ak, String sk, String token) throws Exception {

        log.info("[AliOssService] init OSS by config: {},{},{},{},{}", endpoint, mode, ak, sk, token);

        CredentialsProvider credentialsProvider;
        CredentialType credentialType = CredentialType.parse(mode);
        switch (credentialType) {
            case PWD:
                credentialsProvider = new DefaultCredentialProvider(ak, sk, token);
                break;
            case SYSTEM_PROPERTY:
                credentialsProvider = CredentialsProviderFactory.newSystemPropertiesCredentialsProvider();
                break;
            default:
                credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        }

        this.oss = new OSSClientBuilder().build(endpoint, credentialsProvider);
        log.info("[AliOssService] initialize OSS successfully!");
    }


    @AllArgsConstructor
    enum CredentialType {
        /**
         * 从环境读取
         */
        ENV("env"),
        /**
         * 系统配置
         */
        SYSTEM_PROPERTY("sys"),
        /**
         * 从账号密码读取
         */
        PWD("pwd")
        ;

        private final String code;

        /**
         * parse credential type
         * @param mode oms.storage.dfs.alioss.credential_type
         * @return CredentialType
         */
        public static CredentialType parse(String mode) {

            for (CredentialType credentialType : values()) {
                if (StringUtils.equalsIgnoreCase(credentialType.code, mode)) {
                    return credentialType;
                }
            }

            return PWD;
        }

    }
}
