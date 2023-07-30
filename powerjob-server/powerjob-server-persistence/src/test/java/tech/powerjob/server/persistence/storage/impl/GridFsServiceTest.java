package tech.powerjob.server.persistence.storage.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.server.extension.dfs.DFsService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * test GridFS
 *
 * @author tjq
 * @since 2023/7/30
 */
@Slf4j
class GridFsServiceTest extends AbstractDfsServiceTest {

    @Override
    protected Optional<DFsService> fetchService() {

        try {
            // 后续本地测试，密钥相关的内容统一存入 .powerjob_test 中，方便管理
            String content = FileUtils.readFileToString(new File(System.getProperty("user.home").concat("/.powerjob_test")), StandardCharsets.UTF_8);
            if (StringUtils.isNotEmpty(content)) {
                JSONObject jsonObject = JSONObject.parseObject(content);
                Object mongoUri = jsonObject.get("mongoUri");
                if (mongoUri != null) {
                    GridFsService gridFsService = new GridFsService();
                    gridFsService.initMongo(String.valueOf(mongoUri));

                    return Optional.of(gridFsService);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("[GridFsServiceTest] fetch mongo config failed, skip!");
        }
        return Optional.empty();
    }
}