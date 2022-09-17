package tech.powerjob.official.processors.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.official.processors.util.CommonUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BroadcastProcessor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * 配置处理器
 * 超简易的配置中心，用于配置的下发，需要配合秒级 + 广播任务使用！
 * 超低成本下的解决方案，强配置 or 高SLA 场景，请使用标准的配置管理中间件。
 * 外部调用方法 {@link ConfigProcessor#fetchConfig()}
 *
 * @author tjq
 * @since 2022/9/17
 */
@Slf4j
public class ConfigProcessor implements BroadcastProcessor {

    /**
     * 获取配置
     * @return 控制台下发的配置
     */
    public static Map<String, Object> fetchConfig() {
        if (config == null) {
            return Maps.newHashMap();
        }
        return Optional.ofNullable(config.getConfig()).orElse(Maps.newHashMap());
    }

    private static Config config;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        Config newCfg = JsonUtils.parseObject(CommonUtils.parseParams(context), Config.class);
        context.getOmsLogger().info("[ConfigProcessor] receive and update config: {}", config);

        // 空场景不更新
        final Map<String, Object> realConfig = newCfg.config;
        if (realConfig == null) {
            return new ProcessResult(false, "CONFIG_IS_NULL");
        }

        config = newCfg;

        if (StringUtils.isNotEmpty(config.persistentFileName)) {
            final File file = new File(config.persistentFileName);

            String content = JSONObject.toJSONString(realConfig);
            FileUtils.copyToFile(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), file);
        }

        return new ProcessResult(true, "UPDATE_SUCCESS");
    }

    @Data
    public static class Config implements Serializable {

        /**
         * 原始配置
         */
        private Map<String, Object> config;

        /**
         * 持久到本地的全路径名称
         */
        private String persistentFileName;
    }
}
