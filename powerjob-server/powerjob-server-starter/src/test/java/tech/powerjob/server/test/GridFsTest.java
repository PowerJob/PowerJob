package tech.powerjob.server.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tech.powerjob.server.persistence.mongodb.GridFsManager;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GridFsTest {

    @Resource
    private GridFsManager gridFsManager;

    @Test
    @Disabled
    public void testStore() throws IOException {
        /**
        File file = new File("/Users/tjq/Desktop/DistributeCompute/oms-template-origin.zip");
        gridFsManager.store(file, "test", "test.zip");
         **/
    }

    @Test
    @Disabled
    public void testDownload() throws IOException {
        /**
        File file = new File("/Users/tjq/Desktop/tmp/test-download.zip");
        gridFsManager.download(file, "test", "test.zip");
         **/
    }

    @Test
    @Disabled
    public void testDelete() {
        /**
        gridFsManager.deleteBefore("fs", 0);
         **/
    }

    @Test
    @Disabled
    public void testExists() {
        /**
        System.out.println(gridFsManager.exists("test", "test.zip"));
        System.out.println(gridFsManager.exists("test", "oms-sql.sql"));
         **/
    }

}
