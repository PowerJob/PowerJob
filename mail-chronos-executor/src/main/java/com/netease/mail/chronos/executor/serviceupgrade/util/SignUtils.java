package com.netease.mail.chronos.executor.serviceupgrade.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Echo009
 */
@Slf4j
public class SignUtils {


    public static final String SIGN = "sign";

    private SignUtils() {
    }

    public static String md5Sign(Map<String, String> params, String salt) {
        TreeMap<String, String> treeParams = new TreeMap<>(params);
        StringBuilder source = new StringBuilder();
        for (Map.Entry<String, String> param :  treeParams.entrySet()) {

            if (param.getValue() == null || SIGN.equals(param.getKey())) {
                continue;
            }
            source.append(param.getKey()).append("=").append(param.getValue()).append("&");
        }
        return md5(salt, source.toString());
    }

    public static String md5(String salt, String paramSource) {
        String source = paramSource + salt;
        String sign = MD5Utils.md5(source);
        log.info("[cmd:md5,paramSource:{},sign:{}]", paramSource, sign);
        return sign;
    }


}
