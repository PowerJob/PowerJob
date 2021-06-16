package tech.powerjob.server.common.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtils {

    /**
     * 加密算法RSA
     */
    public static final String KEY_ALGORITHM = "RSA";

    //public static final String RSA_ALGORITHM = "RSA/None/PKCS1Padding";

    /**
     * RSA密钥长度，RSA算法的默认密钥长度是1024
     * 密钥长度必须是64的倍数，在512到65536位之间
     */
    public static final int KEY_SIZE = 1024;
    /**
     * RSA最大加密明文大小
     */
    public static final int MAX_ENCRYPT_BLOCK = 117;
    /**
     * RSA最大解密密文大小
     */
    public static final int MAX_DECRYPT_BLOCK = 128;

    /**
     * 编码方式
     */
    public static final String ENCODING_UTF8 = "UTF-8";

    /**
     * 生成公私密钥对
     *
     * @return
     */
    public static RSAKey generateKeyPair() {
        RSAKey rsaKey = new RSAKey();
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGen.initialize(KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyPairGen.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            //加密数据长度 <= 模长 - 11
            System.out.println("加密数据长度：" + (publicKey.getModulus().bitLength() / 8 - 11));
            //解密数据长度 <= 模长
            System.out.println("解密数据长度：" + (privateKey.getModulus().bitLength() / 8));

            rsaKey.setPublic_key(Base64.encodeBase64String(keyPair.getPublic().getEncoded()));
            rsaKey.setPrivate_key(Base64.encodeBase64String(keyPair.getPrivate().getEncoded()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return rsaKey;
    }

    /**
     * 公钥加密
     *
     * @param data
     * @param public_key
     * @return
     * @throws Exception
     */
    public static byte[] encrypt(byte[] data, String public_key) throws Exception {
        if (data == null) {
            return data;
        }

        if (public_key == null || "".equals(public_key.trim())) {
            return data;
        }

        //生成公钥
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(Base64.decodeBase64(public_key));
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PublicKey publicKey = keyFactory.generatePublic(x509KeySpec);

        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        //对数据分段加密
        byte[] encryptedData = null;
        int length = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        try {
            while (length - offSet > 0) {
                if (length - offSet > MAX_ENCRYPT_BLOCK) {
                    cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(data, offSet, length - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_ENCRYPT_BLOCK;
            }

            encryptedData = out.toByteArray();
        } finally {
            out.close();
        }

        return encryptedData;
    }

    /**
     * 公钥加密字符串
     *
     * @param data
     * @param public_key
     * @return
     * @throws Exception
     */
    public static String encryptStr(String data, String public_key) throws Exception {
        if (data == null) {
            return data;
        }
        return Base64.encodeBase64String(RSAUtils.encrypt(data.getBytes(ENCODING_UTF8), public_key));
    }

    /**
     * 私钥解密
     *
     * @param data
     * @param private_key
     * @return
     * @throws Exception
     */
    public static byte[] decrypt(byte[] data, String private_key) throws Exception {
        if (data == null) {
            return data;
        }
        if (private_key == null || "".equals(private_key.trim())) {
            return data;
        }
        //生成私钥
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(private_key));
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);

        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        //对数据分段解密
        byte[] decryptedData = null;
        int length = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        try {
            while (length - offSet > 0) {
                if (length - offSet > MAX_DECRYPT_BLOCK) {
                    cache = cipher.doFinal(data, offSet, MAX_DECRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(data, offSet, length - offSet);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet = i * MAX_DECRYPT_BLOCK;
            }

            decryptedData = out.toByteArray();
        } finally {
            out.close();
        }

        return decryptedData;
    }

    /**
     * 私钥解密字符串
     *
     * @param data
     * @param private_key
     * @return
     * @throws Exception
     */
    public static String decryptStr(String data, String private_key) throws Exception {
        if (data == null) {
            return data;
        }
        return new String(decrypt(Base64.decodeBase64(data.getBytes(ENCODING_UTF8)), private_key), "UTF-8");
    }

    static class RSAKey {

        private String public_key;
        private String private_key;

        public String getPublic_key() {
            return public_key;
        }

        public void setPublic_key(String public_key) {
            this.public_key = public_key;
        }

        public String getPrivate_key() {
            return private_key;
        }

        public void setPrivate_key(String private_key) {
            this.private_key = private_key;
        }
    }

    public static void main(String args[]) {
        RSAKey generateKeyPair = RSAUtils.generateKeyPair();
        System.out.println("PriKey:" + generateKeyPair.getPrivate_key());
        System.out.println("PubKey:" + generateKeyPair.getPublic_key());
    }
}
