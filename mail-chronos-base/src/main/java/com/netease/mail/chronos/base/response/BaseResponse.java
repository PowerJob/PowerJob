package com.netease.mail.chronos.base.response;

import com.netease.mail.chronos.base.enums.BaseStatusEnum;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Echo009
 * @since 2021/9/18
 */
@Data
@Accessors(chain = true)
public class BaseResponse<T> {
    /**
     * 状态码
     */
    private int code;
    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 状态简述
     */
    private String desc;
    /**
     * 结果
     */
    private T result;

    private String error;

    public static <T> BaseResponse<T> success(T result) {
        BaseResponse<T> rtn = new BaseResponse<>();
        rtn.setCode(BaseStatusEnum.SUCCESS.getCode());
        rtn.setDesc(BaseStatusEnum.SUCCESS.getDesc());
        rtn.setSuccess(true);
        rtn.setResult(result);
        return rtn;
    }

}
