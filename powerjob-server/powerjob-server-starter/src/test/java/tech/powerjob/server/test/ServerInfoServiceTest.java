package tech.powerjob.server.test;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;
import tech.powerjob.server.persistence.remote.model.ServerInfoDO;
import tech.powerjob.server.persistence.remote.repository.ServerInfoRepository;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * test server info
 *
 * @author tjq
 * @since 2021/2/21
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServerInfoServiceTest {

    @Resource
    private ServerInfoRepository serverInfoRepository;

    @Test
    @Transactional
    @Rollback
    public void generateInvalidRecord2Test() {

        List<ServerInfoDO> records = Lists.newLinkedList();
        for (int i = 0; i < 11; i++) {

            // invalid ip to test
            String ip = "T-192.168.1." + i;

            Date gmtModified = DateUtils.addHours(new Date(), -ThreadLocalRandom.current().nextInt(1, 48));

            ServerInfoDO serverInfoDO = new ServerInfoDO(ip);
            serverInfoDO.setGmtModified(gmtModified);

            records.add(serverInfoDO);
        }

        serverInfoRepository.saveAll(records);
        serverInfoRepository.flush();
    }

}