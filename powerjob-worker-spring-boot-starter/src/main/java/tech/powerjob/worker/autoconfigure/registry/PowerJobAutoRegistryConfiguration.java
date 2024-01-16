package tech.powerjob.worker.autoconfigure.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import tech.powerjob.client.IPowerJobClient;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.common.request.http.SaveJobInfoRequest;
import tech.powerjob.common.request.query.JobInfoQuery;
import tech.powerjob.common.response.JobInfoDTO;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.worker.annotation.ProcessRegistrar;
import tech.powerjob.worker.autoconfigure.PowerJobAutoConfiguration;
import tech.powerjob.worker.autoconfigure.PowerJobProperties;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

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
@ConditionalOnClass(IPowerJobClient.class)
@Configuration
@EnableConfigurationProperties(PowerJobRegistryProperties.class)
@AutoConfigureAfter(PowerJobAutoConfiguration.class)
@RequiredArgsConstructor
public class PowerJobAutoRegistryConfiguration implements InitializingBean {

    /**
     * 被注入spring的bean
     */
    private final ObjectProvider<List<BasicProcessor>> processorList;

    private final PowerJobProperties powerJobProperties;

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public void afterPropertiesSet() {
        List<ImmutablePair<BasicProcessor, ProcessRegistrar>> collect = processorList.getIfAvailable(Collections::emptyList)
                .stream()
                .map(basicProcessor -> {
                    Class<?> userClass = ClassUtils.getUserClass(basicProcessor);
                    ProcessRegistrar annotation = AnnotationUtils.findAnnotation(userClass, ProcessRegistrar.class);
                    ImmutablePair<BasicProcessor, ProcessRegistrar> nullPair = ImmutablePair.nullPair();
                    return annotation == null ? nullPair : ImmutablePair.of(basicProcessor, annotation);
                })
                .filter(e -> !ImmutablePair.nullPair().equals(e))
                .collect(Collectors.toList());
        if (collect.isEmpty()) {
            return;
        }
        //检查app信息
        PowerJobProperties.Worker worker = powerJobProperties.getWorker();
        String serverAddress = worker.getServerAddress();

        String appName = worker.getAppName();

        String[] serverAddresses = serverAddress.split(",");

        for (String address : serverAddresses) {
            PowerJobClient powerJobClient = new PowerJobClient(address, appName, appName);
            for (ImmutablePair<BasicProcessor, ProcessRegistrar> immutablePair : collect) {

                //查询job是否存在
                ProcessRegistrar right = immutablePair.getRight();
                BasicProcessor left = immutablePair.getLeft();
                String classFileName = ClassUtils.getUserClass(left).getName();
                String tag = right.uniqueTag();
                JobInfoQuery jobInfoQuery = new JobInfoQuery();
                jobInfoQuery.setTagEq(tag);
                jobInfoQuery.setProcessorInfoEq(classFileName);
                ResultDTO<List<JobInfoDTO>> listResultDTO = powerJobClient.queryJob(jobInfoQuery);
                List<JobInfoDTO> data = listResultDTO.getData();
                String jobName = right.name() + "(于" + DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss") + "自动创建)";

                if (data.isEmpty()) {
                    //注册
                    SaveJobInfoRequest saveJobInfoRequest = new SaveJobInfoRequest();
                    saveJobInfoRequest.setJobName(jobName);
                    saveJobInfoRequest.setJobDescription(right.description());
                    saveJobInfoRequest.setTimeExpressionType(right.timeExpressionType());
                    saveJobInfoRequest.setTimeExpression(right.timeExpression());
                    saveJobInfoRequest.setExecuteType(right.executeType());
                    saveJobInfoRequest.setProcessorType(right.processorType());
                    saveJobInfoRequest.setProcessorInfo(classFileName);
                    saveJobInfoRequest.setEnable(right.enableAfterRegister());
                    saveJobInfoRequest.setExtra(applicationName);
                    saveJobInfoRequest.setTag(tag);
                    powerJobClient.saveJob(saveJobInfoRequest);
                    log.info("[PowerJobRegistrar] Automatic registration successful process({}),jobName:({}),serverAddress:({})", classFileName, jobName, address);
                } else {
                    log.warn("[PowerJobRegistrar] Auto registration failed, job already exists, process({}),jobName:({}),serverAddress:({})", classFileName, jobName, address);

                }
            }


        }
    }

}
