package tech.powerjob.server.test;

import org.junit.jupiter.api.Disabled;
import tech.powerjob.server.core.alarm.impl.DingTalkUtils;
import org.junit.jupiter.api.Test;

/**
 * 测试钉钉消息工具
 *
 * @author tjq
 * @since 2020/8/8
 */
public class DingTalkTest {

    private static final Long AGENT_ID = 847044348L;
    private static final DingTalkUtils dingTalkUtils = new DingTalkUtils("dingauqwkvxxnqskknfv", "XWrEPdAZMPgJeFtHuL0LH73LRj-74umF2_0BFcoXMfvnX0pCQvt0rpb1JOJU_HLl");

    @Test
    @Disabled
    public void testFetchUserId() throws Exception {
        /**
        System.out.println(dingTalkUtils.fetchUserIdByMobile("38353"));
         **/
    }

    @Test
    @Disabled
    public void testSendMarkdown() throws Exception {
        /**
        String userId = "2159453017839770,1234";

        List<DingTalkUtils.MarkdownEntity> mds = Lists.newLinkedList();
        mds.add(new DingTalkUtils.MarkdownEntity("t111","hahahahahahahha1"));
        mds.add(new DingTalkUtils.MarkdownEntity("t2222","hahahahahahahha2"));
        mds.add(new DingTalkUtils.MarkdownEntity("t3333","hahahahahahahha3"));

        dingTalkUtils.sendMarkdownAsync("PowerJob AlarmService", mds, userId, AGENT_ID);
         **/
    }

}
