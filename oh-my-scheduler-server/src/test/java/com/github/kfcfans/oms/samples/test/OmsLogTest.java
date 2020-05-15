package com.github.kfcfans.oms.samples.test;

import com.github.kfcfans.oms.samples.service.log.InstanceLogCleanService;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Date;
import java.util.function.Consumer;

/**
 * 在线日志测试
 *
 * @author tjq
 * @since 2020/5/11
 */

@ActiveProfiles("daily")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OmsLogTest {

    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private GridFsTemplate gridFsTemplate;
    @Resource
    private InstanceLogCleanService instanceLogCleanService;

    @Test
    public void testLocalLogCleaner() {
        instanceLogCleanService.cleanLocal();
    }

    @Test
    public void testRemoteLogCleaner() {
        instanceLogCleanService.cleanRemote();
    }

    @Test
    public void testGridFsQuery() {
        Query mongoQuery = Query.query(Criteria.where("uploadDate").gt(new Date()));
        gridFsTemplate.find(mongoQuery).forEach(new Consumer<GridFSFile>() {
            @Override
            public void accept(GridFSFile gridFSFile) {
                System.out.println(gridFSFile.getFilename());
            }
        });
    }
}
