package com.github.kfcfans.oms.server.web.controller;

import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.ProcessorType;
import com.github.kfcfans.oms.server.common.constans.TimeExpressionType;
import com.github.kfcfans.oms.server.persistence.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.pojo.ResultDTO;
import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.web.request.ModifyJobInfoRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 任务信息管理 Controller
 *
 * @author tjq
 * @since 2020/3/30
 */
@RestController()
@RequestMapping("job")
public class JobController {

    @Resource
    private JobInfoRepository jobInfoRepository;

    @PostMapping("/save")
    public ResultDTO<Void> saveJobInfo(ModifyJobInfoRequest request) {

        JobInfoDO jobInfoDO = new JobInfoDO();
        BeanUtils.copyProperties(request, jobInfoDO);

        // 拷贝枚举值
        jobInfoDO.setExecuteType(ExecuteType.valueOf(request.getExecuteType()).getV());
        jobInfoDO.setProcessorType(ProcessorType.valueOf(request.getProcessorType()).getV());
        jobInfoDO.setTimeExpressionType(TimeExpressionType.valueOf(request.getTimeExpression()).getV());

        jobInfoRepository.saveAndFlush(jobInfoDO);
        return ResultDTO.success(null);
    }

    @GetMapping("/delete")
    public ResultDTO<Void> deleteJobInfo(Long jobId) {
        jobInfoRepository.deleteById(jobId);
        return ResultDTO.success(null);
    }

}
