package tech.powerjob.server.test;

import tech.powerjob.server.persistence.mongodb.GridFsManager;
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
    private GridFsManager gridFsManager;

    @Test
    public void testStore() throws IOException {
        File file = new File("/Users/tjq/Desktop/DistributeCompute/oms-template-origin.zip");
        gridFsManager.store(file, "test", "test.zip");
    }

    @Test
    public void testDownload() throws IOException {
        File file = new File("/Users/tjq/Desktop/tmp/test-download.zip");
        gridFsManager.download(file, "test", "test.zip");
    }

    @Test
    public void testDelete() {
        gridFsManager.deleteBefore("fs", 0);
    }

    @Test
    public void testExists() {
        System.out.println(gridFsManager.exists("test", "test.zip"));
        System.out.println(gridFsManager.exists("test", "oms-sql.sql"));
    }
}
