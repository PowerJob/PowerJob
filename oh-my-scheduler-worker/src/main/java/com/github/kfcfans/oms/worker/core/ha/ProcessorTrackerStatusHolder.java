package com.github.kfcfans.oms.worker.core.ha;

import com.github.kfcfans.oms.worker.common.constants.CommonSJ;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorTrackerStatusReportReq;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * 统一管理 ProcessorTracker 的状态
 *
 * @author tjq
 * @since 2020/3/28
 */
public class ProcessorTrackerStatusHolder {

    private final Map<String, ProcessorTrackerStatus> ip2Status;

    public ProcessorTrackerStatusHolder(String allWorkerAddress) {

        ip2Status = Maps.newConcurrentMap();

        List<String> addressList = CommonSJ.commaSplitter.splitToList(allWorkerAddress);
        addressList.forEach(ip -> {
            ProcessorTrackerStatus pts = new ProcessorTrackerStatus();
            pts.init(ip);
            ip2Status.put(ip, pts);
        });
    }

    public ProcessorTrackerStatus getProcessorTrackerStatus(String ip) {
        return ip2Status.get(ip);
    }

    /**
     * 根据 ProcessorTracker 的心跳更新状态
     */
    public void updateStatus(ProcessorTrackerStatusReportReq heartbeatReq) {
        ProcessorTrackerStatus processorTrackerStatus = ip2Status.get(heartbeatReq.getIp());
        processorTrackerStatus.update(heartbeatReq);
    }

    /**
     * 获取可用 ProcessorTracker 的IP地址
     */
    public List<String> getAvailableProcessorTrackers() {

        List<String> result = Lists.newLinkedList();
        ip2Status.forEach((ip, ptStatus) -> {
            if (ptStatus.available()) {
                result.add(ip);
            }
        });
        return result;
    }

    /**
     * 获取所有 ProcessorTracker 的IP地址（包括不可用状态）
     */
    public List<String> getAllProcessorTrackers() {
        return Lists.newArrayList(ip2Status.keySet());
    }

    /**
     * 获取所有失联 ProcessorTracker 的IP地址
     */
    public List<String> getAllDisconnectedProcessorTrackers() {

        List<String> result = Lists.newLinkedList();
        ip2Status.forEach((ip, ptStatus) -> {
            if (ptStatus.isTimeout()) {
                result.add(ip);
            }
        });
        return result;
    }
}
