package com.netease.mail.chronos.executor.serviceupgrade.param;

import com.alibaba.fastjson.JSON;
import com.netease.mail.chronos.base.exception.BaseException;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Echo009
 * @since 2022/1/6
 */
@Data
@Accessors(chain = true)
public class ServUpgradeParam {
    /**
     * 操作的资源
     */
    private String resource;
    /**
     * 操作类型 commit / rollback
     */
    private String operateType;
    /**
     * 操作资源的参数
     */
    private ApiParam param;


    public void validate() {
        if (StringUtils.isBlank(operateType)) {
            throw new BaseException("缺失 operateType 参数");
        }
        if (!"commit".equals(operateType) && !"rollback".equals(operateType)) {
            throw new BaseException("非法的 operateType 参数：" + operateType);
        }
        if (StringUtils.isBlank(param.getAccount()) || StringUtils.isBlank(param.getToken()) || StringUtils.isBlank(param.getResource()) || param.getStrategy() == null){
            throw new BaseException("API Param 无效，"+ JSON.toJSONString(param));
        }
    }

}
