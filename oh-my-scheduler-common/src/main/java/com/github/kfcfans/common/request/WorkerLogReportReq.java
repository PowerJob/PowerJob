package com.github.kfcfans.common.request;

import com.github.kfcfans.common.OmsSerializable;
import com.github.kfcfans.common.model.InstanceLogContent;
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
    private List<InstanceLogContent> instanceLogContents;
}
