package tech.powerjob.server.remote.worker.utils;

import org.junit.jupiter.api.Test;
import tech.powerjob.server.common.module.WorkerInfo;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SpecifyUtilsTest
 *
 * @author tjq
 * @since 2024/2/24
 */
class SpecifyUtilsTest {

    @Test
    void match() {

        WorkerInfo workerInfo = new WorkerInfo();
        workerInfo.setAddress("192.168.1.1");
        workerInfo.setTag("tag1");

        assert SpecifyUtils.match(workerInfo, "192.168.1.1");
        assert SpecifyUtils.match(workerInfo, "192.168.1.1,192.168.1.2,192.168.1.3,192.168.1.4");

        assert !SpecifyUtils.match(workerInfo, "172.168.1.1");
        assert !SpecifyUtils.match(workerInfo, "172.168.1.1,172.168.1.2,172.168.1.3");

        assert SpecifyUtils.match(workerInfo, "tag1");
        assert SpecifyUtils.match(workerInfo, "tag1,tag2");
        assert !SpecifyUtils.match(workerInfo, "t1");
        assert !SpecifyUtils.match(workerInfo, "t1,t2");

        assert SpecifyUtils.match(workerInfo, "tagIn:tag1");
        assert !SpecifyUtils.match(workerInfo, "tagIn:tag2");

        assert SpecifyUtils.match(workerInfo, "tagEquals:tag1");
        assert !SpecifyUtils.match(workerInfo, "tagEquals:tag2");

        workerInfo.setTag("tag1,tag2,tag3");
        assert SpecifyUtils.match(workerInfo, "tagIn:tag1");
        assert SpecifyUtils.match(workerInfo, "tagIn:tag3");
        assert !SpecifyUtils.match(workerInfo, "tagIn:tag99");
    }
}