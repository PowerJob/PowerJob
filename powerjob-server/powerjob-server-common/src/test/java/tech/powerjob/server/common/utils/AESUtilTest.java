package tech.powerjob.server.common.utils;

import org.junit.jupiter.api.Test;

/**
 * AESUtilTest
 *
 * @author tjq
 * @since 2024/8/10
 */
class AESUtilTest {

    @Test
    void testAes() throws Exception {

        String sk = "ChinaNo.1_ChinaNo.1_ChinaNo.1";

        String txt = "kyksjdfh";

        String encrypt = AESUtil.encrypt(txt, sk);
        System.out.println(encrypt);
        String decrypt = AESUtil.decrypt(encrypt, sk);
        System.out.println(decrypt);

        assert txt.equals(decrypt);
    }

}