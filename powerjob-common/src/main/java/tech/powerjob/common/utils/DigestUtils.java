package tech.powerjob.common.utils;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * 加密工具
 *
 * @author tjq
 * @since 2023/3/25
 */
public class DigestUtils {

    /**
     * 32位小写 md5
     * @param input 输入
     * @return md5
     */
    @SneakyThrows
    public static String md5(String input) {

        if (StringUtils.isEmpty(input)) {
            return null;
        }

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(input.getBytes());
        byte[] byteArray = md5.digest();

        BigInteger bigInt = new BigInteger(1, byteArray);
        // 参数16表示16进制
        StringBuilder result = new StringBuilder(bigInt.toString(16));
        // 不足32位高位补零
        while(result.length() < 32) {
            result.insert(0, "0");
        }
        return result.toString();
    }

    public static String rePassword(String password, String salt) {
        String f1 = String.format("%s_%s_z", salt, password);
        return String.format("%s_%s_b", salt, md5(f1));
    }
}
