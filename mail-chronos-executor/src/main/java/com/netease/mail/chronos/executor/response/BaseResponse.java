package com.netease.mail.chronos.executor.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Echo009
 * @since 2021/8/25
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
public class BaseResponse<T> {

    private boolean success;

    private String message;

    private T data;

    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>(true, "SUCCESS", null);
    }

}
