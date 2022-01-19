package tech.powerjob.server.extension.defaultimpl.alarm.module;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 报警内容
 *
 * @author tjq
 * @since 2020/8/1
 */
public interface Alarm extends PowerSerializable {
    /**
     * 获取标题信息
     * @return title
     */
    String fetchTitle();

    /**
     * 获取简要的报警信息
     * @return simple content
     */
    String fetchSimpleContent();

    default String fetchContent() {
        StringBuilder sb = new StringBuilder();
        JSONObject content = JSON.parseObject(JSON.toJSONString(this));
        content.forEach((key, originWord) -> {
            sb.append(key).append(": ");
            String word = String.valueOf(originWord);
            if (StringUtils.endsWithIgnoreCase(key, "time") || StringUtils.endsWithIgnoreCase(key, "date")) {
                try {
                    if (originWord instanceof Long) {
                        word = CommonUtils.formatTime((Long) originWord);
                    }
                } catch (Exception ignore) {
                    //
                }
            }
            sb.append(word).append(OmsConstant.LINE_SEPARATOR);
        });
        return sb.toString();
    }

    Map<String, String> fetchContentMap();
}
