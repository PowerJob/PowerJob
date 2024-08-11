package tech.powerjob.client.service;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

/**
 * HTTP 响应
 *
 * @author tjq
 * @since 2024/8/10
 */
@Data
@Accessors(chain = true)
public class HttpResponse implements Serializable {

    private boolean success;

    private int code;

    private String response;

    private Map<String, String> headers;
}
