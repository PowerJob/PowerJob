package com.github.kfcfans.oms.samples.web.response;

import lombok.Data;

/**
 * 系统概览
 *
 * @author tjq
 * @since 2020/4/14
 */
@Data
public class SystemOverviewVO {
    private long jobCount;
    private long runningInstanceCount;
    private long failedInstanceCount;
}
