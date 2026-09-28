package tech.powerjob.server.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tech.powerjob.common.enums.*;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.server.core.service.impl.job.JobConverter;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;

import static org.junit.jupiter.api.Assertions.*;

class JobExportStatusTest {
    @ParameterizedTest
    @EnumSource(SwitchableStatus.class)
    void exportPreservesEnabledStateWithoutReactivatingDisabledOrDeletedHistory(SwitchableStatus status) {
        JobInfoDO job = new JobInfoDO(); job.setId(101L); job.setAppId(201L); job.setJobName("export fixture");
        job.setStatus(status.getV()); job.setTimeExpressionType(TimeExpressionType.API.getV());
        job.setExecuteType(ExecuteType.STANDALONE.getV()); job.setProcessorType(ProcessorType.BUILT_IN.getV());
        job.setProcessorInfo("fixture.Processor"); job.setJobParams("中文 &+%\\n");
        SaveJobInfoRequest exported = JobConverter.convertJobInfoDO2SaveJobInfoRequest(job);
        assertEquals(status == SwitchableStatus.ENABLE, exported.isEnable());
        assertEquals(job.getJobParams(), exported.getJobParams());
        assertEquals(job.getProcessorInfo(), exported.getProcessorInfo());
        assertEquals(status.getV(), job.getStatus());
    }
}
