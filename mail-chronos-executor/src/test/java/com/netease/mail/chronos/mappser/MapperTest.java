package com.netease.mail.chronos.mappser;

import com.netease.mail.chronos.base.DaoBaseContext;
import com.netease.mail.chronos.executor.support.entity.SpRtTaskInstance;
import com.netease.mail.chronos.executor.support.mapper.SpRtTaskInstanceMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Echo009
 * @since 2021/10/26
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DaoBaseContext.class})
@ActiveProfiles(value = "local")
public class MapperTest {

    @Autowired
    private SpRtTaskInstanceMapper spRtTaskInstanceMapper;

    @Test
    public void testSpRtTaskInstanceMapper(){

        SpRtTaskInstance spRtTaskInstance = spRtTaskInstanceMapper.selectById(1);

    }

}
