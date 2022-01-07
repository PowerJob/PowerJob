package com.netease.mail.chronos.context.srvupgrade;

import lombok.Data;
import lombok.experimental.Accessors;
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




}
