package com.github.kfcfans.oms.server.test;

import com.github.kfcfans.oms.server.persistence.mongodb.GridFsHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

/**
 * GridFS 测试
 *
 * @author tjq
 * @since 2020/5/18
 */
@ActiveProfiles("daily")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GridFsTest {

    @Resource
    private GridFsHelper gridFsHelper;

    @Test
    public void testStore() throws IOException {
        File file = new File("/Users/tjq/Desktop/DistributeCompute/oms-template-origin.zip");
        gridFsHelper.store(file, "test", "test.zip");
    }

    @Test
    public void testDownload() throws IOException {
        File file = new File("/Users/tjq/Desktop/tmp/test-download.zip");
        gridFsHelper.download(file, "test", "test.zip");
    }

    @Test
    public void testDelete() {
        gridFsHelper.deleteBefore("fs", 0);
    }
}
