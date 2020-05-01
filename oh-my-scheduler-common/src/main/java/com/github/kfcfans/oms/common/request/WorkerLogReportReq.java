package com.github.kfcfans.oms.common.request;

import com.github.kfcfans.oms.common.OmsSerializable;
import com.github.kfcfans.oms.common.model.InstanceLogContent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 日志上报请求
 *
 * @author tjq
 * @since 2020/4/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerLogReportReq implements OmsSerializable {
    private String workerAddress;
    private List<InstanceLogContent> instanceLogContents;
}
