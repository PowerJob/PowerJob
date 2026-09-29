package tech.powerjob.server.openapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.server.core.instance.InstanceService;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.core.service.CacheService;
import tech.powerjob.server.core.service.JobService;
import tech.powerjob.server.core.workflow.WorkflowInstanceService;
import tech.powerjob.server.core.workflow.WorkflowService;
import tech.powerjob.server.openapi.security.OpenApiSecurityService;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.web.ControllerExceptionHandler;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class OpenApiLegacyContractTest {

    private JobService jobs;
    private InstanceService instances;
    private CacheService cache;
    private OpenApiInterceptor interceptor;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        jobs = mock(JobService.class);
        instances = mock(InstanceService.class);
        cache = mock(CacheService.class);
        OpenApiSecurityService security = mock(OpenApiSecurityService.class);
        interceptor = new OpenApiInterceptor();
        ReflectionTestUtils.setField(interceptor, "openApiSecurityService", security);
        ReflectionTestUtils.setField(interceptor, "enableOpenApiAuth", true);
        OpenAPIController controller = new OpenAPIController(mock(AppInfoService.class), jobs, instances,
                mock(WorkflowService.class), mock(WorkflowInstanceService.class), security, cache);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ControllerExceptionHandler()).addInterceptors(interceptor).build();
        when(jobs.fetchAllJob(anyLong())).thenReturn(Collections.emptyList());
        when(jobs.queryJob(any())).thenReturn(Collections.emptyList());
        when(jobs.saveJob(any())).thenReturn(303L);
        when(cache.getAppIdByJobId(303L)).thenReturn(202L);
        when(cache.getAppIdByInstanceId(404L)).thenReturn(202L);
        JobInfoDO copied = new JobInfoDO();
        copied.setId(505L);
        copied.setAppId(202L);
        when(jobs.copyJob(303L)).thenReturn(copied);
        when(instances.getInstanceStatus(404L)).thenReturn(InstanceStatus.WAITING_DISPATCH);
        InstanceInfoDTO info = new InstanceInfoDTO();
        info.setAppId(202L);
        info.setInstanceId(404L);
        when(instances.getInstanceInfo(404L)).thenReturn(info);
    }

    private MockHttpServletRequestBuilder authenticated(String path) {
        return post("/openApi/" + path).header("X-POWERJOB-APP-ID", "101")
                .header("X-POWERJOB-ACCESS-TOKEN", "accepted-fixture-token");
    }

    @Test
    void matchingAppKeepsExistingReadContract() throws Exception {
        mvc.perform(authenticated("fetchAllJob").param("appId", "101"))
                .andExpect(jsonPath("$.success").value(true));
        verify(jobs).fetchAllJob(101L);
    }

    @Test
    void authenticationDisabledRetainsLegacyAppParameter() throws Exception {
        ReflectionTestUtils.setField(interceptor, "enableOpenApiAuth", false);
        mvc.perform(post("/openApi/fetchAllJob").param("appId", "202"))
                .andExpect(jsonPath("$.success").value(true));
        verify(jobs).fetchAllJob(202L);
    }

    @Test
    void authenticatedRequestRetainsExplicitFormApp() throws Exception {
        mvc.perform(authenticated("fetchAllJob").param("appId", "202"))
                .andExpect(jsonPath("$.success").value(true));
        verify(jobs).fetchAllJob(202L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"appIdEq\":202}", "{}"})
    void queryRetainsLegacyExplicitOrUnboundedScope(String json) throws Exception {
        mvc.perform(authenticated("queryJob").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(jsonPath("$.success").value(true));
        verify(jobs).queryJob(any());
    }

    @Test
    void saveRetainsLegacyBodyApp() throws Exception {
        mvc.perform(authenticated("saveJob").contentType(MediaType.APPLICATION_JSON).content("{\"appId\":202}"))
                .andExpect(jsonPath("$.success").value(true));
        verify(jobs).saveJob(any());
    }

    @Test
    void copyRetainsIdOnlyLookup() throws Exception {
        mvc.perform(authenticated("copyJob").param("jobId", "303").param("appId", "101"))
                .andExpect(jsonPath("$.success").value(true));
        verify(jobs).copyJob(303L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fetchInstanceInfo", "fetchInstanceStatus"})
    void instanceReadsRetainIdOnlyLookup(String endpoint) throws Exception {
        mvc.perform(authenticated(endpoint).param("instanceId", "404").param("appId", "101"))
                .andExpect(jsonPath("$.success").value(true));
        if (endpoint.equals("fetchInstanceInfo")) verify(instances).getInstanceInfo(404L);
        else verify(instances).getInstanceStatus(404L);
    }
}
