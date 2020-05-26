package com.github.kfcfans.oms.server.service.workflow;

import com.github.kfcfans.oms.server.persistence.core.model.WorkflowInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.WorkflowInfoRepository;
import com.github.kfcfans.oms.server.service.DispatchService;
import com.google.common.collect.Maps;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 管理运行中的工作流实例
 *
 * @author tjq
 * @since 2020/5/26
 */
@Service
public class WorkflowInstanceManager {

    @Resource
    private DispatchService dispatchService;

    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private WorkflowInfoRepository workflowInfoRepository;


    private final Map<Long, WorkflowInfoDO> wfId2Info = Maps.newConcurrentMap();

    public void submit(WorkflowInfoDO wfInfo) {
        wfId2Info.put(wfInfo.getId(), wfInfo);
    }
}
