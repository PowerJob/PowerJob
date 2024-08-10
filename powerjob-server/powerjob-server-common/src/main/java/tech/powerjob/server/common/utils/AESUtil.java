package tech.powerjob.server.common.utils;

import lombok.SneakyThrows;
import tech.powerjob.common.utils.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {


    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256; // AES 256-bit
    private static final int GCM_NONCE_LENGTH = 12; // GCM nonce length (12 bytes)
    private static final int GCM_TAG_LENGTH = 16;  // GCM authentication tag length (16 bytes)

    // SecureRandom 实例，用于生成 nonce
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成密钥
     *
     * @param key 传入的密钥字符串，必须是 32 字节（256 位）长度
     * @return SecretKeySpec 实例
     */
    private static SecretKeySpec getKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        // 不足 32 字节，则使用 MD5 转为 32 位
        if (keyBytes.length != KEY_SIZE / 8) {
            keyBytes = DigestUtils.md5(key).getBytes(StandardCharsets.UTF_8);
        }
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * 加密
     *
     * @param data 要加密的数据
     * @param key  加密密钥
     * @return 加密后的数据（Base64 编码），包含 nonce
     */
    @SneakyThrows
    public static String encrypt(String data, String key) {
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        secureRandom.nextBytes(nonce); // 生成随机的 nonce

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, getKey(key), gcmParameterSpec);

        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // 将 nonce 和密文连接在一起，然后进行 Base64 编码
        byte[] combinedData = new byte[nonce.length + encryptedData.length];
        System.arraycopy(nonce, 0, combinedData, 0, nonce.length);
        System.arraycopy(encryptedData, 0, combinedData, nonce.length, encryptedData.length);

        return Base64.getEncoder().encodeToString(combinedData);
    }

    /**
     * 解密
     *
     * @param encryptedData 要解密的数据（Base64 编码），包含 nonce
     * @param key           解密密钥
     * @return 解密后的数据
     */
    @SneakyThrows
    public static String decrypt(String encryptedData, String key) {
        byte[] combinedData = Base64.getDecoder().decode(encryptedData);

        // 提取 nonce
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        System.arraycopy(combinedData, 0, nonce, 0, nonce.length);

        // 提取实际的加密数据
        byte[] encryptedText = new byte[combinedData.length - nonce.length];
        System.arraycopy(combinedData, nonce.length, encryptedText, 0, encryptedText.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        cipher.init(Cipher.DECRYPT_MODE, getKey(key), gcmParameterSpec);

        byte[] decryptedData = cipher.doFinal(encryptedText);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}
