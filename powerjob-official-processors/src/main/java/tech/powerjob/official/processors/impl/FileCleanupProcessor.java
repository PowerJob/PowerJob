package tech.powerjob.official.processors.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.core.processor.sdk.BroadcastProcessor;
import com.github.kfcfans.powerjob.worker.log.OmsLogger;
import com.google.common.base.Stopwatch;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * common file(like logs) cleaner
 *
 * @author tjq
 * @since 2021/2/1
 */
public class FileCleanupProcessor extends BroadcastProcessor {

    @Override
    public ProcessResult process(TaskContext taskContext) throws Exception {

        OmsLogger logger = taskContext.getOmsLogger();
        logger.info("[FileCleanupProcessor] using params: {}", taskContext.getJobParams());

        Stopwatch sw = Stopwatch.createStarted();

        List<CleanupParams> cleanupParamsList = JSONArray.parseArray(taskContext.getJobParams(), CleanupParams.class);

        cleanupParamsList.forEach(params -> {

            logger.info("[FileCleanupProcessor] start to process: {}", JSON.toJSON(params));

            if (StringUtils.isEmpty(params.filePattern) || StringUtils.isEmpty(params.dirPath)) {
                logger.warn("[FileCleanupProcessor] skip due to invalid params!");
                return;
            }
            File dir = new File(params.dirPath);
            if (!dir.exists()) {
                logger.warn("[FileCleanupProcessor] skip due to dirPath[{}] not exists", params.dirPath);
                return;
            }
            if (!dir.isDirectory()) {
                logger.warn("[FileCleanupProcessor] skip due to dirPath[{}] is not a directory", params.dirPath);
                return;
            }

            logger.info("[FileCleanupProcessor] start to search directory: {}", params.dirPath);
            Collection<File> files = FileUtils.listFiles(dir, null, true);
            logger.info("[FileCleanupProcessor] total file num: {}", files.size());

            Pattern filePattern = Pattern.compile(params.filePattern);

            files.forEach(file -> {

                String fileName = file.getName();

                if (!filePattern.matcher(fileName).matches()) {
                    logger.info("[FileCleanupProcessor] file[{}] won't be deleted due to filename not match the pattern: {}", fileName, params.filePattern);
                    return;
                }

                // last modify time interval, xxx hours
                int interval = (int) Math.ceil((System.currentTimeMillis() - file.lastModified()) / 3600000.0);
                if (interval < params.retentionTime) {
                    logger.info("[FileCleanupProcessor] file[{}] won't be deleted because it does not meet the time requirement", fileName);
                    return;
                }

                try {
                    FileUtils.forceDelete(file);
                    logger.info("[FileCleanupProcessor] delete file[{}] successfully!", fileName);
                } catch (Exception e) {
                    logger.error("[FileCleanupProcessor] delete file[{}] failed!", fileName, e);
                }

            });
        });

        return new ProcessResult(true, String.format("cost:%s", sw.toString()));
    }

    @Data
    public static class CleanupParams {
        private String dirPath;
        private String filePattern;
        private Integer retentionTime;
    }
}
