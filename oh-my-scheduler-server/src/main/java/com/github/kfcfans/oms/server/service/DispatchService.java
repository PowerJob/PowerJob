package com.github.kfcfans.oms.server.service;

import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.JobLogRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 派送服务
 *
 * @author tjq
 * @since 2020/4/5
 */
@Service
public class DispatchService {

    @Resource
    private JobLogRepository jobLogRepository;

    public void dispatch(JobInfoDO jobInfo) {

        // 1. 查询当前运行的实例数


    }
}
