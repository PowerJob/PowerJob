package tech.powerjob.server.persistence.storage.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.minio.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import tech.powerjob.server.common.spring.condition.PropertyAndOneBeanCondition;
import tech.powerjob.server.extension.dfs.*;
import tech.powerjob.server.persistence.storage.AbstractDFsService;

import javax.annotation.Priority;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MINIO support
 * <a href="https://min.io/">High Performance Object Storage</a>
 * 配置项：
 * oms.storage.dfs.minio.endpoint
 * oms.storage.dfs.minio.bucketName
 * oms.storage.dfs.minio.accessKey
 * oms.storage.dfs.minio.secretKey
 *
 * @author xinyi
 * @since 2023/8/21
 */
@Slf4j
@Priority(value = Integer.MAX_VALUE - 3)
@Conditional(MinioOssService.MinioOssCondition.class)
public class MinioOssService extends AbstractDFsService {

    private static final String TYPE_MINIO = "minio";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_BUCKET_NAME = "bucketName";
    private static final String ACCESS_KEY = "accessKey";
    private static final String SECRET_KEY = "secretKey";
    private MinioClient minioClient;
    private String bucket;
    private static final String NO_SUCH_KEY = "NoSuchKey";

    @Override
    public void store(StoreRequest storeRequest) {
        try {
            minioClient.uploadObject(UploadObjectArgs.builder()
                    .bucket(this.bucket)
                    .object(parseFileName(storeRequest.getFileLocation()))
                    .filename(storeRequest.getLocalFile().getPath())
                    .contentType(Files.probeContentType(storeRequest.getLocalFile().toPath()))
                    .build());
        } catch (Throwable t) {
            ExceptionUtils.rethrow(t);
        }
    }

    @Override
    public void download(DownloadRequest downloadRequest) {
        try {
            FileUtils.forceMkdirParent(downloadRequest.getTarget());
            // 下载文件
            minioClient.downloadObject(
                    DownloadObjectArgs.builder()
                            .bucket(this.bucket)
                            .object(parseFileName(downloadRequest.getFileLocation()))
                            .filename(downloadRequest.getTarget().getAbsolutePath())
                            .build());
        } catch (Throwable t) {
            ExceptionUtils.rethrow(t);
        }
    }

    /**
     * 获取文件元
     *
     * @param fileLocation 文件位置
     */
    @Override
    public Optional<FileMeta> fetchFileMeta(FileLocation fileLocation) {
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(this.bucket)
                    .object(parseFileName(fileLocation))
                    .build());
            return Optional.ofNullable(stat).map(minioStat -> {

                Map<String, Object> metaInfo = Maps.newHashMap();
                if (stat.userMetadata() != null) {
                    metaInfo.putAll(stat.userMetadata());
                }
                return new FileMeta()
                        .setLastModifiedTime(Date.from(stat.lastModified().toInstant()))
                        .setLength(stat.size())
                        .setMetaInfo(metaInfo);
            });
        } catch (Exception oe) {
            String errorCode = oe.getMessage();
            if (NO_SUCH_KEY.equalsIgnoreCase(errorCode)) {
                return Optional.empty();
            }
            ExceptionUtils.rethrow(oe);
        }
        return Optional.empty();
    }

    private static String parseFileName(FileLocation fileLocation) {
        return String.format("%s/%s", fileLocation.getBucket(), fileLocation.getName());
    }

    /**
     * 清理过期文件
     *
     * @param bucket 桶名
     * @param days   日期
     */
    @Override
    public void cleanExpiredFiles(String bucket, int days) {
        /*
        使用Minio的管理界面或Minio客户端命令行工具设置对象的生命周期规则。在生命周期规则中定义文件的过期时间。Minio将自动根据设置的规则删除过期的文件。
         */
    }

    /**
     * 释放连接
     */
    @Override
    public void destroy() {
        //minioClient.close();
    }

    /**
     * 初始化minio
     *
     * @param applicationContext /
     */
    @Override
    protected void init(ApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();

        String endpoint = fetchProperty(environment, TYPE_MINIO, KEY_ENDPOINT);
        String bucketName = fetchProperty(environment, TYPE_MINIO, KEY_BUCKET_NAME);
        String accessKey = fetchProperty(environment, TYPE_MINIO, ACCESS_KEY);
        String secretKey = fetchProperty(environment, TYPE_MINIO, SECRET_KEY);

        try {
            initOssClient(endpoint, bucketName, accessKey, secretKey);
        } catch (Exception e) {
            ExceptionUtils.rethrow(e);
        }
    }

    /**
     * 创建minio连接并且创建桶
     *
     * @param endpoint   端口
     * @param bucketName 桶名
     * @param accessKey  访问密钥
     * @param secretKey  秘密密钥
     */
    public void initOssClient(String endpoint, String bucketName, String accessKey, String secretKey) {
        log.info("[Minio] init OSS by config: endpoint={}, bucketName={}, accessKey={}, secretKey={}", endpoint, bucketName, accessKey, secretKey);
        if (StringUtils.isEmpty(bucketName)) {
            throw new IllegalArgumentException("'oms.storage.dfs.minio.bucketName' can't be empty, please creat a bucket in minio oss console then config it to powerjob");
        }
        this.bucket = bucketName;
        minioClient = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        createBucket(bucketName);
        log.info("[Minio] initialize OSS successfully!");
    }

    /**
     * 创建 bucket
     *
     * @param bucketName 桶名
     */
    @SneakyThrows(Exception.class)
    public void createBucket(String bucketName) {
        if (!bucketExists(bucketName)) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName).build());
        }
        String policy = "{\n" +
                "    \"Version\": \"2012-10-17\",\n" +
                "    \"Statement\": [\n" +
                "        {\n" +
                "            \"Action\": [\n" +
                "                \"s3:GetObject\"\n" +
                "            ],\n" +
                "            \"Effect\": \"Allow\",\n" +
                "            \"Principal\": {\n" +
                "                \"AWS\": [\n" +
                "                    \"*\"\n" +
                "                ]\n" +
                "            },\n" +
                "            \"Resource\": [\n" +
                "                \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
    }

    /**
     * 判断 bucket是否存在
     *
     * @param bucketName: 桶名
     * @return boolean
     */
    @SneakyThrows(Exception.class)
    public boolean bucketExists(String bucketName) {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    public static class MinioOssCondition extends PropertyAndOneBeanCondition {

        @Override
        protected List<String> anyConfigKey() {
            return Lists.newArrayList("oms.storage.dfs.minio.endpoint");
        }

        @Override
        protected Class<?> beanType() {
            return DFsService.class;
        }
    }
}
