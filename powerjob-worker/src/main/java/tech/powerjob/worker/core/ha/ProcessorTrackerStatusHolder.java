package tech.powerjob.worker.core.ha;

import tech.powerjob.worker.pojo.request.ProcessorTrackerStatusReportReq;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 统一管理 ProcessorTracker 的状态
 *
 * @author tjq
 * @since 2020/3/28
 */
@Slf4j
public class ProcessorTrackerStatusHolder {

    private final Long instanceId;
    private final Integer maxWorkerCount;
    // ProcessorTracker的address(IP:Port) -> 状态
    private final Map<String, ProcessorTrackerStatus> address2Status;

    public ProcessorTrackerStatusHolder(Long instanceId, Integer maxWorkerCount, List<String> allWorkerAddress) {

        this.instanceId = instanceId;
        this.maxWorkerCount = maxWorkerCount;

        address2Status = Maps.newConcurrentMap();
        allWorkerAddress.forEach(address -> {
            ProcessorTrackerStatus pts = new ProcessorTrackerStatus();
            pts.init(address);
            address2Status.put(address, pts);
        });
    }

    /**
     * 根据地址获取 ProcessorTracker 的状态
     * @param address IP:Port
     * @return status
     */
    public ProcessorTrackerStatus getProcessorTrackerStatus(String address) {
        // remove 前突然收到了 PT 心跳同时立即被派发才可能出现这种情况，0.001% 概率
        return address2Status.computeIfAbsent(address, ignore -> {
            log.warn("[PTStatusHolder-{}] unregistered worker: {}", instanceId, address);
            ProcessorTrackerStatus processorTrackerStatus = new ProcessorTrackerStatus();
            processorTrackerStatus.init(address);
            return processorTrackerStatus;
        });
    }

    /**
     * 根据 ProcessorTracker 的心跳更新状态
     */
    public void updateStatus(ProcessorTrackerStatusReportReq heartbeatReq) {
        getProcessorTrackerStatus(heartbeatReq.getAddress()).update(heartbeatReq);
    }

    /**
     * 获取可用 ProcessorTracker 的IP地址
     */
    public List<String> getAvailableProcessorTrackers() {

        List<String> result = Lists.newLinkedList();
        address2Status.forEach((address, ptStatus) -> {
            if (ptStatus.available()) {
                result.add(address);
            }
        });
        return result;
    }

    /**
     * 获取所有 ProcessorTracker 的IP地址（包括不可用状态）
     */
    public List<String> getAllProcessorTrackers() {
        return Lists.newArrayList(address2Status.keySet());
    }

    /**
     * 获取所有失联 ProcessorTracker 的IP地址
     */
    public List<String> getAllDisconnectedProcessorTrackers() {

        List<String> result = Lists.newLinkedList();
        address2Status.forEach((ip, ptStatus) -> {
            if (ptStatus.isTimeout()) {
                result.add(ip);
            }
        });
        return result;
    }

    /**
     * 注册新的执行节点
     * @param address 新的执行节点地址
     * @return true: register successfully / false: already exists
     */
    private boolean registerOne(String address) {
        ProcessorTrackerStatus pts = address2Status.get(address);
        if (pts != null) {
            return false;
        }
        pts = new ProcessorTrackerStatus();
        pts.init(address);
        address2Status.put(address, pts);
        log.info("[PTStatusHolder-{}] register new worker: {}", instanceId, address);
        return true;
    }

    public void register(List<String> workerIpList) {
        if (endlessWorkerNum()) {
            workerIpList.forEach(this::registerOne);
            return;
        }
        List<String> availableProcessorTrackers = getAvailableProcessorTrackers();
        int currentWorkerSize = availableProcessorTrackers.size();
        int needMoreNum = maxWorkerCount - currentWorkerSize;
        if (needMoreNum <= 0) {
            return;
        }

        log.info("[PTStatusHolder-{}] currentWorkerSize: {}, needMoreNum: {}", instanceId, currentWorkerSize, needMoreNum);

        for (String newIp : workerIpList) {
            boolean success = registerOne(newIp);
            if (success) {
                needMoreNum --;
            }
            if (needMoreNum <= 0) {
                return;
            }
        }
    }

    /**
     * 检查是否需要动态加载新的执行器
     * @return check need more workers
     */
    public boolean checkNeedMoreWorker() {
        if (endlessWorkerNum()) {
            return true;
        }
        return getAvailableProcessorTrackers().size() < maxWorkerCount;
    }

    private boolean endlessWorkerNum() {
        return maxWorkerCount == null || maxWorkerCount == 0;
    }

    public void remove(List<String> addressList) {
        addressList.forEach(address2Status::remove);
    }
}
