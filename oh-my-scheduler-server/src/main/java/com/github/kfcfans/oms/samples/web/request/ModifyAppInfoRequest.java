package com.github.kfcfans.oms.samples.web.request;

import lombok.Data;

/**
 * 修改应用信息请求
 *
 * @author tjq
 * @since 2020/4/1
 */
@Data
public class ModifyAppInfoRequest {

    private Long id;
    private String appName;
    private String description;
}
