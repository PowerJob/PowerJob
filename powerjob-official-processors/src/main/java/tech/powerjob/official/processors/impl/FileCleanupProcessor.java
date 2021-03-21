package tech.powerjob.official.processors.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import tech.powerjob.official.processors.util.SecurityUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BroadcastProcessor;
import tech.powerjob.worker.log.OmsLogger;
import com.google.common.base.Stopwatch;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;

/**
 * common file(like logs) cleaner
 *
 * @author tjq
 * @since 2021/2/1
 */
public class FileCleanupProcessor implements BroadcastProcessor {

    @Override
    public ProcessResult preProcess(TaskContext context) throws Exception {
        if (SecurityUtils.disable(SecurityUtils.ENABLE_FILE_CLEANUP_PROCESSOR)) {
            String msg = String.format("FileCleanupProcessor is not enabled, please set '-D%s=true' to enable it", SecurityUtils.ENABLE_FILE_CLEANUP_PROCESSOR);
            context.getOmsLogger().warn(msg);
            return new ProcessResult(false, msg);
        }
        return new ProcessResult(true);
    }

    @Override
    public ProcessResult process(TaskContext taskContext) throws Exception {

        OmsLogger logger = taskContext.getOmsLogger();
        logger.info("using params: {}", taskContext.getJobParams());

        LongAdder cleanNum = new LongAdder();
        Stopwatch sw = Stopwatch.createStarted();

        List<CleanupParams> cleanupParamsList = JSONArray.parseArray(taskContext.getJobParams(), CleanupParams.class);

        cleanupParamsList.forEach(params -> {

            logger.info("start to process: {}", JSON.toJSON(params));

            if (StringUtils.isEmpty(params.filePattern) || StringUtils.isEmpty(params.dirPath)) {
                logger.warn("skip due to invalid params!");
                return;
            }
            File dir = new File(params.dirPath);
            if (!dir.exists()) {
                logger.warn("skip due to dirPath[{}] not exists", params.dirPath);
                return;
            }
            if (!dir.isDirectory()) {
                logger.warn("skip due to dirPath[{}] is not a directory", params.dirPath);
                return;
            }

            logger.info("start to search directory: {}", params.dirPath);
            Collection<File> files = FileUtils.listFiles(dir, null, true);
            logger.info("total file num: {}", files.size());

            Pattern filePattern = Pattern.compile(params.filePattern);

            files.forEach(file -> {

                String fileName = file.getName();
                String filePath = file.getAbsolutePath();

                if (!filePattern.matcher(fileName).matches()) {
                    logger.info("file[{}] won't be deleted due to filename not match the pattern: {}", fileName, params.filePattern);
                    return;
                }

                // last modify time interval, xxx hours
                int interval = (int) Math.ceil((System.currentTimeMillis() - file.lastModified()) / 3600000.0);
                if (interval < params.retentionTime) {
                    logger.info("file[{}] won't be deleted because it does not meet the time requirement", filePath);
                    return;
                }

                try {
                    FileUtils.forceDelete(file);
                    cleanNum.increment();
                    logger.info("delete file[{}] successfully!", filePath);
                } catch (Exception e) {
                    logger.error("delete file[{}] failed!", filePath, e);
                }

            });
        });

        return new ProcessResult(true, String.format("cost:%s,clean:%d", sw.toString(), cleanNum.longValue()));
    }

    @Data
    public static class CleanupParams {
        private String dirPath;
        private String filePattern;
        private Integer retentionTime;
    }
}
