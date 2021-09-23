package com.netease.mail.chronos.base.exception;


import com.netease.mail.chronos.base.enums.StatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Echo009
 * @since 2021/9/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseException extends RuntimeException{

    private final Integer code ;

    public BaseException(Throwable throwable){
        super(throwable);
        this.code = 500;
    }

    public BaseException(String message){
        super(message);
        this.code = 500;
    }


    public BaseException(Integer code, String message){
        super(message);
        this.code = code;
    }

    public BaseException(StatusEnum statusEnum){
        super(statusEnum.getDesc());
        this.code = statusEnum.getCode();
    }

    @Override
    public String toString(){
        String s = getClass().getName();
        String message = getLocalizedMessage();
        if (message == null ){
            return s + ": code=" + code;
        }
        return s + ": code = "+code + ",message = " + message;
    }
}
