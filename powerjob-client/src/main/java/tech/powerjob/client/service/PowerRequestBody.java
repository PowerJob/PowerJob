package tech.powerjob.client.service;

import com.google.common.collect.Maps;
import lombok.Getter;
import tech.powerjob.common.enums.MIME;

import java.util.Map;

/**
 * 请求体
 *
 * @author tjq
 * @since 2024/8/10
 */
@Getter
public class PowerRequestBody {

    private MIME mime;

    private Object payload;

    private final Map<String, String> headers = Maps.newHashMap();

    private PowerRequestBody() {
    }

    public static PowerRequestBody newJsonRequestBody(Object data) {
        PowerRequestBody powerRequestBody = new PowerRequestBody();
        powerRequestBody.mime = MIME.APPLICATION_JSON;
        powerRequestBody.payload = data;
        return powerRequestBody;
    }

    public static PowerRequestBody newFormRequestBody(Map<String, String> form) {
        PowerRequestBody powerRequestBody = new PowerRequestBody();
        powerRequestBody.mime = MIME.APPLICATION_FORM;
        powerRequestBody.payload = form;
        return powerRequestBody;
    }

    public void addHeaders(Map<String, String> hs) {
        if (hs == null || hs.isEmpty()) {
            return;
        }
        this.headers.putAll(hs);
    }
}
