package com.netease.mail.chronos.executor.serviceupgrade.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.netease.mail.chronos.base.exception.BaseException;
import com.netease.mail.chronos.context.common.constants.BizParamKeyConstant;
import com.netease.mail.chronos.context.srvupgrade.ServUpgradeParam;
import com.netease.mail.chronos.executor.serviceupgrade.util.SignUtils;
import com.netease.mail.quark.status.StatusResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Echo009
 * @since 2022/1/6
 */
@Component
@Slf4j
public class ServiceCommitOrRollbackProcessor implements BasicProcessor {

    @Value("${srvupgrade.baseUrl}")
    private String baseUrl;

    @Value("${srvupgrade.product:MAIL_CHRONOS}")
    private String product;

    @Value("${srvupgrade.salt:ezuUEA_4tfOze8JYjlY3cB1r}")
    private String salt;

    private static final String COMMIT_PATH = "/s/commit";

    private static final String ROLLBACK_PATH = "/s/rollback";

    private static final int TIMEOUT = 60;

    private static final int HTTP_SUCCESS_CODE = 200;


    @Override
    @SneakyThrows
    public ProcessResult process(TaskContext context) {

        OmsLogger omsLogger = context.getOmsLogger();
        // 根据 静态参数 决定处理的资源
        if (StringUtils.isBlank(context.getJobParams())) {
            return new ProcessResult(false, "必须指定需要处理的资源！");
        }
        String targetResource = StringUtils.trim(context.getJobParams());
        ServUpgradeParam servUpgradeParam = obtainParam(targetResource, context, omsLogger);
        // 构造参数
        String operateType = servUpgradeParam.getOperateType();
        HashMap<String, String> params = new HashMap<>(16);
        fillParam(servUpgradeParam, operateType, params);

        omsLogger.info("final params:{}", JSON.toJSONString(params));
        // send request
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ZERO)
                .readTimeout(Duration.ZERO)
                .writeTimeout(Duration.ZERO)
                .callTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();


        Request req = new Request.Builder().url(constructUrl(params, operateType)).method("GET", null).build();
        Response response = httpClient.newCall(req).execute();
        omsLogger.info("response: {}", response);
        if (response.code() != HTTP_SUCCESS_CODE) {
            return new ProcessResult(false, "HTTP ERROR");
        }
        if (response.body() == null) {
            omsLogger.error("response body is null ");
            return new ProcessResult(false);
        }
        omsLogger.info("response body : {}", response.body().string());
        StatusResult statusResult = JSON.parseObject(response.body().string(), StatusResult.class);
        omsLogger.info("status result : {}", JSON.toJSONString(statusResult));
        return statusResult != null && statusResult.getCode() == HTTP_SUCCESS_CODE ? new ProcessResult(true) : new ProcessResult(false);

    }

    private String constructUrl(HashMap<String, String> params, String operateType) {
        StringBuilder stringBuilder = new StringBuilder(1024);
        stringBuilder.append(baseUrl);
        if ("commit".equals(operateType)) {
            stringBuilder.append(COMMIT_PATH);
        } else {
            stringBuilder.append(ROLLBACK_PATH);
        }
        stringBuilder.append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            stringBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return stringBuilder.toString();
    }

    private void fillParam(ServUpgradeParam servUpgradeParam, String operateType, HashMap<String, String> params) {
        params.put("token", servUpgradeParam.getParam().getToken());
        params.put("strategy", String.valueOf(servUpgradeParam.getParam().getStrategy()));
        params.put("resource", servUpgradeParam.getParam().getResource());
        params.put("account", servUpgradeParam.getParam().getAccount());
        params.put("product", product);

        if ("commit".equals(operateType)) {
            if (servUpgradeParam.getParam().getExtInfo() != null) {
                params.put("extInfo", servUpgradeParam.getParam().getExtInfo());
            }
        } else {
            if (servUpgradeParam.getParam().getCommitToken() != null) {
                params.put("commitToken", servUpgradeParam.getParam().getCommitToken());
            }
        }

        String sign = SignUtils.md5Sign(params, salt);
        params.put("sign", sign);
    }

    /**
     * 工作流中的参数定义 形式如下
     * {
     * "srv-upgrade":"{"resource":$ServUpgradeParam.JSON}"
     * }
     */

    private ServUpgradeParam obtainParam(String targetResource, TaskContext taskContext, OmsLogger omsLogger) {
        // 解析实例（上下文）参数 , 按照通用标准定义，这里使用业务固有 key ： srv-upgrade
        ServUpgradeParam param = null;
        if (taskContext.getWorkflowContext().getWfInstanceId() != null) {
            Map<String, String> data = taskContext.getWorkflowContext().getData();
            String paramStr = data.get(BizParamKeyConstant.SRV_UPGRADE);
            try {
                HashMap<String, ServUpgradeParam> realParam = JSON.parseObject(paramStr, new TypeReference<HashMap<String, ServUpgradeParam>>() {
                });
                param = realParam.get(targetResource);
            } catch (Exception e) {
                // ignore
            }
            if (param == null) {
                omsLogger.error("从工作流中解析服务升降级参数失败! 原始参数信息:{}", taskContext.getInstanceParams());
                throw new BaseException("从任务实例参数中解析服务升降级参数失败");
            }
        } else {
            String instanceParams = taskContext.getInstanceParams();
            try {
                param = JSON.parseObject(instanceParams, ServUpgradeParam.class);
            } catch (Exception e) {
                omsLogger.error("从任务实例参数中解析服务升降级参数失败! 原始参数信息:{}", taskContext.getInstanceParams(), e);
                throw new BaseException("从任务实例参数中解析服务升降级参数失败");
            }
        }
        validate(param);
        return param;
    }

    private void validate(ServUpgradeParam servUpgradeParam) {
        if (StringUtils.isBlank(servUpgradeParam.getOperateType())) {
            throw new BaseException("缺失 operateType 参数");
        }
        if (!"commit".equals(servUpgradeParam.getOperateType()) && !"rollback".equals(servUpgradeParam.getOperateType())) {
            throw new BaseException("非法的 operateType 参数：" + servUpgradeParam.getOperateType());
        }
        if (StringUtils.isBlank(servUpgradeParam.getParam().getAccount()) || StringUtils.isBlank(servUpgradeParam.getParam().getToken()) || StringUtils.isBlank(servUpgradeParam.getParam().getResource()) || servUpgradeParam.getParam().getStrategy() == null) {
            throw new BaseException("API Param 无效，" + JSON.toJSONString(servUpgradeParam.getParam()));
        }
    }
}
