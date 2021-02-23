package com.github.kfcfans.powerjob.server.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.common.ProcessorType;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.server.extension.LockService;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.JobInfoRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import java.util.List;
import java.util.Set;

/**
 * Help users upgrade from a low version of powerjob-server to a high version of powerjob-server
 * v4 means that this interface was upgraded from version v3.x to v4.x, and so on
 *
 * @author tjq
 * @since 2021/2/23
 */
@Slf4j
@RestController
@RequestMapping("/migrate")
public class MigrateController {

    @Resource
    private LockService lockService;
    @Resource
    private JobInfoRepository jobInfoRepository;

    @GetMapping("/v4/script")
    public ResultDTO<JSONObject> migrateScriptFromV3ToV4(Long appId) {

        JSONObject resultLog = new JSONObject();
        resultLog.put("docs", "https://www.yuque.com/powerjob/guidence/official_processor");
        resultLog.put("tips", "please add the maven dependency of 'powerjob-official-processors'");

        String lock = "migrateScriptFromV3ToV4-" + appId;
        boolean getLock = lockService.tryLock(lock, 60000);
        if (!getLock) {
            return ResultDTO.failed("get lock failed, maybe other migrate job is running");
        }
        try {
            Set<Long> convertedJobIds = Sets.newHashSet();

            Specification<JobInfoDO> specification = (Specification<JobInfoDO>) (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = Lists.newLinkedList();
                List<Integer> scriptJobTypes = Lists.newArrayList(ProcessorType.SHELL.getV(), ProcessorType.PYTHON.getV());
                predicates.add(criteriaBuilder.equal(root.get("appId"), appId));
                predicates.add(root.get("processorType").in(scriptJobTypes));
                return query.where(predicates.toArray(new Predicate[0])).getRestriction();
            };
            List<JobInfoDO> scriptJobs = jobInfoRepository.findAll(specification);

            resultLog.put("scriptJobsNum", scriptJobs.size());
            resultLog.put("convertedJobIds", convertedJobIds);

            log.info("[MigrateScriptFromV3ToV4] script job num: {}", scriptJobs.size());
            scriptJobs.forEach(job -> {

                ProcessorType oldProcessorType = ProcessorType.of(job.getProcessorType());

                job.setJobParams(job.getProcessorInfo());
                job.setProcessorType(ProcessorType.EMBEDDED_JAVA.getV());

                if (oldProcessorType == ProcessorType.PYTHON) {
                    job.setProcessorInfo("tech.powerjob.official.processors.impl.script.PythonProcessor");
                } else {
                    job.setProcessorInfo("tech.powerjob.official.processors.impl.script.ShellProcessor");
                }

                jobInfoRepository.saveAndFlush(job);
                convertedJobIds.add(job.getId());
            });
            return ResultDTO.success(resultLog);
        } finally {
            lockService.unlock(lock);
        }
    }

}
