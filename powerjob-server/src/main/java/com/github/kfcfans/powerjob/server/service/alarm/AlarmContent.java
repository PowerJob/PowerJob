package com.github.kfcfans.powerjob.server.service.alarm;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.common.OmsSerializable;
import com.github.kfcfans.powerjob.common.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 报警内容
 *
 * @author tjq
 * @since 2020/8/1
 */
public interface AlarmContent extends OmsSerializable {

    default String fetchContent() {
        StringBuilder sb = new StringBuilder();
        JSONObject content = JSONObject.parseObject(JSONObject.toJSONString(this));
        content.forEach((key, originWord) -> {
            sb.append(key).append(": ");
            String word = String.valueOf(originWord);
            if (StringUtils.endsWithIgnoreCase(key, "time") || StringUtils.endsWithIgnoreCase(key, "date")) {
                try {
                    if (originWord instanceof Long) {
                        word = CommonUtils.formatTime((Long) originWord);
                    }
                }catch (Exception ignore) {
                }
            }
            sb.append(word).append("\n\r");
        });
        return sb.toString();
    }
}
