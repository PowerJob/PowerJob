package tech.powerjob.server.core.service;

import tech.powerjob.common.PowerQuery;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;

import java.util.List;

/**
 * JobService
 *
 * @author tjq
 * @since 2023/3/4
 */
public interface JobService {

    Long saveJob(SaveJobInfoRequest request);

    JobInfoDO copyJob(Long jobId);

    JobInfoDTO fetchJob(Long jobId);

    List<JobInfoDTO> fetchAllJob(Long appId);

    List<JobInfoDTO> queryJob(PowerQuery powerQuery);

    long runJob(Long appId, Long jobId, String instanceParams, Long delay);

    void deleteJob(Long jobId);

    void disableJob(Long jobId);

    void enableJob(Long jobId);

    SaveJobInfoRequest exportJob(Long jobId);
}
