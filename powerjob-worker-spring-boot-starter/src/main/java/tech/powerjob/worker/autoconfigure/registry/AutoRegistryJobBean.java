package tech.powerjob.worker.autoconfigure.registry;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.ClassUtils;
import tech.powerjob.client.IPowerJobClient;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.request.query.JobInfoQuery;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.worker.autoconfigure.PowerJobProperties;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.sdk.ProcessRegistry;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 要开启自动注册需要引入client的包,就会自动注册
 *
 * @author minsin/mintonzhang@163.com
 * @since 2024/1/16
 */
@Slf4j
@RequiredArgsConstructor
public class AutoRegistryJobBean implements InitializingBean {

    /**
     * 被注入spring的bean
     */
    private final ObjectProvider<List<BasicProcessor>> processorList;

    private final PowerJobProperties powerJobProperties;
    private final IPowerJobClient powerJobClient;


    @Override
    public void afterPropertiesSet() {
        List<ProcessRegistry> collect = processorList.getIfAvailable(Collections::emptyList)
                .stream()
                .filter(ProcessRegistry.class::isInstance)
                .map(ProcessRegistry.class::cast)
                .collect(Collectors.toList());
        if (collect.isEmpty()) {
            return;
        }

        //检查app信息
        PowerJobProperties.Worker worker = powerJobProperties.getWorker();
        String appName = worker.getAppName();


        for (ProcessRegistry registry : collect) {
            Class<?> userClass = ClassUtils.getUserClass(registry);
            String classFileName = userClass.getName();

            String jobName;
            if ("DEFAULT".equals(registry.name())) {
                jobName = appName + '#' + userClass.getSimpleName();
            } else {
                jobName = registry.name();
            }

            //根据tag和process 搜索job是否存在
            JobInfoQuery jobInfoQuery = new JobInfoQuery();
            jobInfoQuery.setTagEq(worker.getAppName());
            jobInfoQuery.setProcessorInfoEq(classFileName);
            jobInfoQuery.setStatusIn(Lists.newArrayList(1));
            ResultDTO<List<JobInfoDTO>> listResultDTO = powerJobClient.queryJob(jobInfoQuery);
            List<JobInfoDTO> data = listResultDTO.getData();


            if (data.isEmpty()) {
                //注册
                SaveJobInfoRequest saveJobInfoRequest = new SaveJobInfoRequest();
                saveJobInfoRequest.setJobName(jobName);
                saveJobInfoRequest.setJobDescription(registry.description());
                saveJobInfoRequest.setTimeExpressionType(registry.timeExpression().getTimeExpressionType());
                saveJobInfoRequest.setTimeExpression(registry.timeExpression().getTimeExpression());
                saveJobInfoRequest.setExecuteType(registry.executeType());
                saveJobInfoRequest.setProcessorType(registry.processorType());
                saveJobInfoRequest.setProcessorInfo(classFileName);
                saveJobInfoRequest.setEnable(registry.enable());
                saveJobInfoRequest.setExtra("Client Auto Registry");
                saveJobInfoRequest.setTag(appName);
                saveJobInfoRequest.setLogConfig(registry.logConfig());
                powerJobClient.saveJob(saveJobInfoRequest);
                log.info("[PowerJobRegistry] 自动注册job成功,process({}),jobName:({})", classFileName, jobName);
            } else {
                log.warn("[PowerJobRegistry] 自动注册job失败,job已经存在, process({}),jobName:({})", classFileName, jobName);
            }
        }


    }
}


