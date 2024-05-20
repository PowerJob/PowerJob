package tech.powerjob.server.persistence.storage.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
    private AmazonS3 amazonS3;
    private String bucket;
    private static final String NOT_FOUNT = "404 Not Found";

    @Override
    public void store(StoreRequest storeRequest) {
        try {

            String fileName = parseFileName(storeRequest.getFileLocation());
            // 创建 PutObjectRequest 对象
            PutObjectRequest request = new PutObjectRequest(this.bucket, fileName, storeRequest.getLocalFile());

            amazonS3.putObject(request);
        } catch (Throwable t) {
            ExceptionUtils.rethrow(t);
        }
    }

    @Override
    public void download(DownloadRequest downloadRequest) {
        try {
            FileUtils.forceMkdirParent(downloadRequest.getTarget());

            String fileName = parseFileName(downloadRequest.getFileLocation());
            GetObjectRequest getObjectRequest = new GetObjectRequest(this.bucket, fileName);
            amazonS3.getObject(getObjectRequest, downloadRequest.getTarget());

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

            String fileName = parseFileName(fileLocation);
            ObjectMetadata objectMetadata = amazonS3.getObjectMetadata(this.bucket, fileName);

            return Optional.ofNullable(objectMetadata).map(minioStat -> {

                Map<String, Object> metaInfo = Maps.newHashMap();

                if (objectMetadata.getRawMetadata() != null) {
                    metaInfo.putAll(objectMetadata.getRawMetadata());
                }
                return new FileMeta()
                        .setLastModifiedTime(objectMetadata.getLastModified())
                        .setLength(objectMetadata.getContentLength())
                        .setMetaInfo(metaInfo);
            });
        } catch (AmazonS3Exception s3Exception) {
            String errorCode = s3Exception.getErrorCode();
            if (NOT_FOUNT.equalsIgnoreCase(errorCode)) {
                return Optional.empty();
            }
        } catch (Exception oe) {
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

        // 创建凭证对象
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

        // 创建AmazonS3客户端并指定终端节点和凭证
        this.amazonS3 = AmazonS3ClientBuilder.standard()
                // 当使用 AWS Java SDK 连接到非AWS服务（如MinIO）时，指定区域（Region）是必需的，即使这个区域对于你的MinIO实例并不真正适用。原因在于AWS SDK的客户端构建器需要一个区域来配置其服务端点，即使在连接到本地或第三方S3兼容服务时也是如此。使用 "us-east-1" 作为占位符是很常见的做法，因为它是AWS最常用的区域之一。这不会影响到实际的连接或数据传输，因为真正的服务地址是由你提供的终端节点URL决定的。如果你的代码主要是与MinIO交互，并且不涉及AWS服务，那么这个区域设置只是形式上的要求。
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withPathStyleAccessEnabled(true) // 重要：启用路径样式访问
                .build();
        this.bucket = bucketName;
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

        // 建议自行创建 bucket，设置好相关的策略
        if (bucketExists(bucketName)) {
           return;
        }

        Bucket createBucketResult = amazonS3.createBucket(bucketName);
        log.info("[Minio] createBucket successfully, bucketName: {}, createResult: {}", bucketName, createBucketResult);

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
        try {
            amazonS3.setBucketPolicy(bucketName, policy);
        } catch (Exception e) {
            log.warn("[Minio] setBucketPolicy failed, maybe you need to setBucketPolicy by yourself!", e);
        }
    }

    /**
     * 判断 bucket是否存在
     *
     * @param bucketName: 桶名
     * @return boolean
     */
    @SneakyThrows(Exception.class)
    public boolean bucketExists(String bucketName) {

        return amazonS3.doesBucketExistV2(bucketName);
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
