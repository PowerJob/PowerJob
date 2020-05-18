package com.github.kfcfans.oms.server.persistence.mongodb;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GridFS 操作助手
 *
 * @author tjq
 * @since 2020/5/18
 */
@Slf4j
@Service
public class GridFsManager {

    private MongoDatabase db;

    private Map<String, GridFSBucket> bucketCache = Maps.newConcurrentMap();

    public static final String LOG_BUCKET = "log";
    public static final String CONTAINER_BUCKET = "container";

    @Autowired(required = false)
    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.db = mongoTemplate.getDb();
    }

    /**
     * 是否可用
     * @return true：可用；false：不可用
     */
    public boolean available() {
        return db != null;
    }

    /**
     * 存储文件到 GridFS
     * @param localFile 本地文件
     * @param bucketName 桶名称
     * @param fileName GirdFS中的文件名称
     * @throws IOException 异常
     */
    public void store(File localFile, String bucketName, String fileName) throws IOException {
        if (db == null) {
            return;
        }
        GridFSBucket bucket = getBucket(bucketName);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(localFile))) {
            bucket.uploadFromStream(fileName, bis);
        }
    }

    /**
     * 从 GridFS 下载文件
     * @param targetFile 下载的目标文件（本地文件）
     * @param bucketName 桶名称
     * @param fileName GirdFS中的文件名称
     * @throws IOException 异常
     */
    public void download(File targetFile, String bucketName, String fileName) throws IOException {
        if (db == null) {
            return;
        }
        GridFSBucket bucket = getBucket(bucketName);
        byte[] buffer = new byte[1024];
        try (GridFSDownloadStream gis = bucket.openDownloadStream(fileName);
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile))
        ) {
            while (gis.read(buffer) != -1) {
                bos.write(buffer);
            }
            bos.flush();
        }
    }

    /**
     * 删除几天前的文件
     * @param bucketName 桶名称
     * @param day 日期偏移量，单位 天
     */
    public void deleteBefore(String bucketName, int day) {

        Stopwatch sw = Stopwatch.createStarted();

        Date date = DateUtils.addDays(new Date(), -day);
        GridFSBucket bucket = getBucket(bucketName);
        Bson filter = Filters.lt("uploadDate", date);

        // 循环删除性能很差？我猜你肯定没看过官方实现[狗头]：org.springframework.data.mongodb.gridfs.GridFsTemplate.delete
        bucket.find(filter).forEach((Consumer<GridFSFile>) gridFSFile -> {
            ObjectId objectId = gridFSFile.getObjectId();
            try {
                bucket.delete(objectId);
                log.info("[GridFsHelper] deleted {}#{}", bucketName, objectId);
            }catch (Exception e) {
                log.error("[GridFsHelper] deleted {}#{} failed.", bucketName, objectId, e);
            }
        });
        log.info("[GridFsHelper] clean bucket({}) successfully, delete all files before {}, using {}.", bucketName, date, sw.stop());
    }

    public boolean exists(String bucketName, String fileName) {
        GridFSBucket bucket = getBucket(bucketName);
        GridFSFindIterable files = bucket.find(Filters.eq("filename", fileName));
        try {
            GridFSFile first = files.first();
            return first != null;
        }catch (Exception ignore) {
        }
        return false;
    }

    private GridFSBucket getBucket(String bucketName) {
        return bucketCache.computeIfAbsent(bucketName, ignore -> GridFSBuckets.create(db, bucketName));
    }
}
