package tech.powerjob.server.common.utils;

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {

    // 加密算法的名称
    private static final String ALGORITHM = "AES";
    // 加密/解密模式和填充方式
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    // 密钥长度（128、192 或 256 位）
    private static final int KEY_SIZE = 128;

    /**
     * 生成密钥
     *
     * @param key 密钥的种子，可以是任意字符串
     * @return 生成的密钥
     * @throws Exception 异常
     */
    private static SecretKeySpec generateKey(String key) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(key.getBytes());
        keyGen.init(KEY_SIZE, secureRandom);
        SecretKey secretKey = keyGen.generateKey();
        return new SecretKeySpec(secretKey.getEncoded(), ALGORITHM);
    }

    /**
     * 加密
     *
     * @param data 要加密的数据
     * @param key  密钥
     * @return 加密后的数据（Base64 编码）
     * @throws Exception 异常
     */
    @SneakyThrows
    public static String encrypt(String data, String key) {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKey = generateKey(key);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * 解密
     *
     * @param encryptedData 要解密的数据（Base64 编码）
     * @param key           密钥
     * @return 解密后的数据
     * @throws Exception 异常
     */
    @SneakyThrows
    public static String decrypt(String encryptedData, String key) {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKey = generateKey(key);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}
