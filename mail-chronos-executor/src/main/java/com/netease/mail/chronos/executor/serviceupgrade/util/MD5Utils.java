package com.netease.mail.chronos.executor.serviceupgrade.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Echo009
 */
public class MD5Utils {

    private MD5Utils() {
    }

    public static String md5(String str) {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.reset();
			messageDigest.update(str.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			return str;
		}
        byte[] byteArray = messageDigest.digest();
		StringBuilder md5StrBuff = new StringBuilder();
        for (byte b : byteArray) {
            md5StrBuff.append("0").append(String.format("%02X", b));
        }
		return md5StrBuff.toString();
	}

}
