package tech.powerjob.worker.persistence.fs.impl;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.CollectionUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.worker.persistence.TaskDO;
import tech.powerjob.worker.persistence.fs.ExternalTaskPersistenceService;
import tech.powerjob.worker.persistence.fs.FsService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 外部文件存储服务
 *
 * @author tjq
 * @since 2024/2/22
 */
@Slf4j
public class ExternalTaskFileSystemPersistenceService implements ExternalTaskPersistenceService {
    private final Long instanceId;

    private final FsService pendingFsService;

    private final FsService resultFsService;

    private static final String PENDING_FILE_NAME = "%d-pending";
    private static final String RESULT_FILE_NAME = "%d-result";

    public ExternalTaskFileSystemPersistenceService(Long instanceId, boolean needResult) {
        this.instanceId = instanceId;

        this.pendingFsService = new LocalDiskFsService(String.format(PENDING_FILE_NAME, instanceId));
        if (needResult) {
            this.resultFsService = new LocalDiskFsService(String.format(RESULT_FILE_NAME, instanceId));
        } else {
            this.resultFsService = new FsService() {
                @Override
                public void writeLine(String content) throws IOException {
                }

                @Override
                public String readLine() throws IOException {
                    return null;
                }
                @Override
                public void close() {
                }
            };
        }
    }

    @Override
    public boolean persistPendingTask(List<TaskDO> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return true;
        }
        try {
            String content = JsonUtils.toJSONString(tasks);
            pendingFsService.writeLine(content);
            return true;
        } catch (Exception e) {
            log.error("[ExternalTaskPersistenceService] [{}] persistPendingTask failed: {}", instanceId, tasks);
        }
        return false;
    }

    @Override
    @SneakyThrows
    public List<TaskDO> readPendingTask() {
        String pendingTaskStr = pendingFsService.readLine();
        return str2TaskDoList(pendingTaskStr);
    }

    @Override
    public boolean persistFinishedTask(List<TaskDO> tasks) {

        if (CollectionUtils.isEmpty(tasks)) {
            return true;
        }

        // 移除无用的参数列
        tasks.forEach(t -> t.setTaskContent(null));

        try {
            String content = JsonUtils.toJSONString(tasks);
            resultFsService.writeLine(content);
            return true;
        } catch (Exception e) {
            log.error("[ExternalTaskPersistenceService] [{}] persistPendingTask failed: {}", instanceId, tasks);
        }
        return false;
    }

    @Override
    @SneakyThrows
    public List<TaskDO> readFinishedTask() {
        String finishedStr = resultFsService.readLine();
        return str2TaskDoList(finishedStr);
    }


    private static List<TaskDO> str2TaskDoList(String finishedStr) throws Exception {
        if (StringUtils.isEmpty(finishedStr)) {
            return Collections.emptyList();
        }
        TaskDO[] taskDOS = JsonUtils.parseObject(finishedStr, TaskDO[].class);
        if (taskDOS != null) {
            return Lists.newArrayList(taskDOS);
        }
        return Collections.emptyList();
    }

    @Override
    public void close()  {
        CommonUtils.executeIgnoreException(() -> {
            if (pendingFsService != null) {
                pendingFsService.close();
            }
        });

        CommonUtils.executeIgnoreException(() -> {
            if (resultFsService != null) {
                resultFsService.close();
            }
        });
    }
}
