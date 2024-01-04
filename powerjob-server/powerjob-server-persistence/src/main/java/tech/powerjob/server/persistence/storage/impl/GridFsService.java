package tech.powerjob.server.persistence.storage.impl;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import tech.powerjob.server.extension.dfs.*;
import tech.powerjob.server.persistence.storage.AbstractDFsService;
import tech.powerjob.server.common.spring.condition.PropertyAndOneBeanCondition;

import javax.annotation.Priority;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 使用 MongoDB GridFS 作为底层存储
 * 配置用法：oms.storage.dfs.mongodb.uri=mongodb+srv://zqq:No1Bug2Please3!@cluster0.wie54.gcp.mongodb.net/powerjob_daily?retryWrites=true&w=majority
 *
 * @author tjq
 * @since 2023/7/28
 */
@Slf4j
@Priority(value = Integer.MAX_VALUE - 10)
@Conditional(GridFsService.GridFsCondition.class)
public class GridFsService extends AbstractDFsService {

    private MongoClient mongoClient;
    private MongoDatabase db;
    private final Map<String, GridFSBucket> bucketCache = Maps.newConcurrentMap();
    private static final String TYPE_MONGO = "mongodb";

    private static final String KEY_URI = "uri";

    private static final String SPRING_MONGO_DB_CONFIG_KEY = "spring.data.mongodb.uri";

    @Override
    public void store(StoreRequest storeRequest) throws IOException {
        GridFSBucket bucket = getBucket(storeRequest.getFileLocation().getBucket());
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(storeRequest.getLocalFile().toPath()))) {
            bucket.uploadFromStream(storeRequest.getFileLocation().getName(), bis);
        }
    }

    @Override
    public void download(DownloadRequest downloadRequest) throws IOException {
        GridFSBucket bucket = getBucket(downloadRequest.getFileLocation().getBucket());
        FileUtils.forceMkdirParent(downloadRequest.getTarget());
        try (GridFSDownloadStream gis = bucket.openDownloadStream(downloadRequest.getFileLocation().getName());
             BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(downloadRequest.getTarget().toPath()))
        ) {
            byte[] buffer = new byte[1024];
            int bytes = 0;
            while ((bytes = gis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytes);
            }
            bos.flush();
        }
    }

    @Override
    public Optional<FileMeta> fetchFileMeta(FileLocation fileLocation) throws IOException {
        GridFSBucket bucket = getBucket(fileLocation.getBucket());
        GridFSFindIterable files = bucket.find(Filters.eq("filename", fileLocation.getName()));
        GridFSFile first = files.first();
        if (first == null) {
            return Optional.empty();
        }
        return Optional.of(new FileMeta()
                .setLength(first.getLength())
                .setLastModifiedTime(first.getUploadDate())
                .setMetaInfo(first.getMetadata()));
    }

    @Override
    public void cleanExpiredFiles(String bucketName, int days) {
        Stopwatch sw = Stopwatch.createStarted();

        Date date = DateUtils.addDays(new Date(), -days);
        GridFSBucket bucket = getBucket(bucketName);
        Bson filter = Filters.lt("uploadDate", date);

        // 循环删除性能很差？我猜你肯定没看过官方实现[狗头]：org.springframework.data.mongodb.gridfs.GridFsTemplate.delete
        bucket.find(filter).forEach(gridFSFile -> {
            ObjectId objectId = gridFSFile.getObjectId();
            try {
                bucket.delete(objectId);
                log.info("[GridFsService] deleted {}#{}", bucketName, objectId);
            }catch (Exception e) {
                log.error("[GridFsService] deleted {}#{} failed.", bucketName, objectId, e);
            }
        });
        log.info("[GridFsService] clean bucket({}) successfully, delete all files before {}, using {}.", bucketName, date, sw.stop());
    }

    private GridFSBucket getBucket(String bucketName) {
        return bucketCache.computeIfAbsent(bucketName, ignore -> GridFSBuckets.create(db, bucketName));
    }

    private String parseMongoUri(Environment environment) {
        // 优先从新的规则读取
        String uri = fetchProperty(environment, TYPE_MONGO, KEY_URI);
        if (StringUtils.isNotEmpty(uri)) {
            return uri;
        }
        // 兼容 4.3.3 前的逻辑，读取 SpringMongoDB 配置
        return environment.getProperty(SPRING_MONGO_DB_CONFIG_KEY);
    }

    void initMongo(String uri) {
        log.info("[GridFsService] mongoDB uri: {}", uri);
        if (StringUtils.isEmpty(uri)) {
            log.warn("[GridFsService] uri is empty, GridFsService is off now!");
            return;
        }

        ConnectionString connectionString = new ConnectionString(uri);
        mongoClient = MongoClients.create(connectionString);

        if (StringUtils.isEmpty(connectionString.getDatabase())) {
            log.warn("[GridFsService] can't find database info from uri, will use [powerjob] as default, please make sure you have created the database 'powerjob'");
        }

        db = mongoClient.getDatabase(Optional.ofNullable(connectionString.getDatabase()).orElse("powerjob"));

        log.info("[GridFsService] initialize MongoDB and GridFS successfully, will use mongodb GridFs as storage layer.");
    }

    @Override
    public void destroy() throws Exception {
        mongoClient.close();
    }

    @Override
    protected void init(ApplicationContext applicationContext) {
        String uri = parseMongoUri(applicationContext.getEnvironment());
        initMongo(uri);

        log.info("[GridFsService] initialize successfully, THIS_WILL_BE_THE_STORAGE_LAYER.");
    }

    public static class GridFsCondition extends PropertyAndOneBeanCondition {
        @Override
        protected List<String> anyConfigKey() {
            return Lists.newArrayList("spring.data.mongodb.uri", "oms.storage.dfs.mongodb.uri");
        }

        @Override
        protected Class<?> beanType() {
            return DFsService.class;
        }
    }
}
