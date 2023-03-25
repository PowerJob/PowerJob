package tech.powerjob.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * md5
 *
 * @author tjq
 * @since 2023/3/25
 */
class DigestUtilsTest {

    @Test
    void testMd5() {
        assert "0a7d83f084ec258aefd128569dda03d7".equals(DigestUtils.md5("6531"));
        assert "7906989e85cbc80207fd0db4b16806f6".equals(DigestUtils.md5("tjq"));
        assert "59cb4db84c02f5bee62fb0e6e02e758d".equals(DigestUtils.md5("PowerJob is a great job scheduling framework!"));
        assert "25adfe55fd639fcfd1c09e57ccddbd33".equals(DigestUtils.md5("HAHA"));
    }
}