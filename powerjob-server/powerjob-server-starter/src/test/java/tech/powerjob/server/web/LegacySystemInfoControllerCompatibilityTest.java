package tech.powerjob.server.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.persistence.remote.repository.InstanceInfoRepository;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;
import tech.powerjob.server.remote.server.self.ServerInfoService;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;
import tech.powerjob.server.web.controller.SystemInfoController;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class LegacySystemInfoControllerCompatibilityTest {
    private WorkerClusterQueryService workers;
    private JobInfoRepository jobs;
    private ServerInfoService servers;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        workers = mock(WorkerClusterQueryService.class);
        jobs = mock(JobInfoRepository.class);
        servers = mock(ServerInfoService.class);
        AppInfoRepository apps = mock(AppInfoRepository.class);
        AppInfoDO app = new AppInfoDO();
        app.setId(20L);
        app.setAppName("legacy-app");
        when(apps.findById(20L)).thenReturn(Optional.of(app));
        when(workers.getAllWorkers(20L)).thenReturn(Collections.emptyList());
        mvc = MockMvcBuilders.standaloneSetup(new SystemInfoController(apps, jobs,
                mock(InstanceInfoRepository.class), servers, workers)).build();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"20", "21"})
    void workerQueryUsesTheLegacyQueryParameter(String header) throws Exception {
        MockHttpServletRequestBuilder request = get("/system/listWorker").param("appId", "20");
        if (header != null) request.header("AppId", header);
        mvc.perform(request).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").isEmpty());
        verify(workers).getAllWorkers(20L);
        verifyNoMoreInteractions(workers);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"20", "21"})
    void overviewRetainsLegacyQueryRoutingInsteadOfReplacingItWithTheHeader(String header) throws Exception {
        MockHttpServletRequestBuilder request = get("/system/overview").param("appId", "20");
        if (header != null) request.header("AppId", header);
        mvc.perform(request).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appId").value(20)).andExpect(jsonPath("$.data.appName").value("legacy-app"));
        verify(jobs).countByAppIdAndStatusNot(eq(20L), anyInt());
        verify(servers).fetchAppServerInfo(20L);
    }
}
