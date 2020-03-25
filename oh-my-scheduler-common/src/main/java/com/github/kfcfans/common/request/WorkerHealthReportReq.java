package com.github.kfcfans.common.request;

import com.github.kfcfans.common.model.SystemMetrics;
import lombok.Data;

import java.io.Serializable;

/**
 * Worker 上报健康信息（worker定时发送的heartbeat）
 *
 * @author tjq
 * @since 2020/3/25
 */
@Data
public class WorkerHealthReportReq implements Serializable {

    // 本机地址 -> IP:port
    private String totalAddress;

    private SystemMetrics systemMetrics;
}
