package com.github.kfcfans.powerjob.worker.core.processor.built;

import com.github.kfcfans.powerjob.worker.common.utils.OmsWorkerFileUtils;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.core.processor.sdk.BasicProcessor;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 脚本处理器
 *
 * @author tjq
 * @since 2020/4/16
 */
@Slf4j
public abstract class ScriptProcessor implements BasicProcessor {

    private final Long instanceId;
    // 脚本绝对路径
    private final String scriptPath;
    private final long timeout;

    private static final Set<String> DOWNLOAD_PROTOCOL = Sets.newHashSet("http", "https", "ftp");

    public ScriptProcessor(Long instanceId, String processorInfo, long timeout) throws Exception {

        this.instanceId = instanceId;
        this.scriptPath = OmsWorkerFileUtils.getScriptDir() + genScriptName(instanceId);
        this.timeout = timeout;

        File script = new File(scriptPath);
        if (script.exists()) {
            return;
        }

        File dir = new File(script.getParent());
        boolean success = dir.mkdirs();
        success = script.createNewFile();
        if (!success) {
            throw new RuntimeException("create script file failed");
        }

        // 如果是下载连接，则从网络获取
        for (String protocol : DOWNLOAD_PROTOCOL) {
            if (processorInfo.startsWith(protocol)) {
                FileUtils.copyURLToFile(new URL(processorInfo), script, 5000, 300000);
                return;
            }
        }

        // 持久化到本地
        try (FileWriter fw = new FileWriter(script); BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(processorInfo);
            bw.flush();
        }
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        // 1. 授权
        ProcessBuilder chmodPb = new ProcessBuilder("/bin/chmod", "755", scriptPath);
        // 等待返回，这里不可能导致死锁（shell产生大量数据可能导致死锁）
        chmodPb.start().waitFor();

        // 2. 执行目标脚本
        ProcessBuilder pb = new ProcessBuilder(fetchRunCommand(), scriptPath);
        Process process = pb.start();

        StringBuilder inputSB = new StringBuilder();
        StringBuilder errorSB = new StringBuilder();

        // 为了代码优雅而牺牲那么一点点点点点点点点性能
        // 从外部传入线程池总感觉怪怪的...内部创建嘛又要考虑考虑资源释放问题，想来想去还是直接创建算了。
        new Thread(() -> copyStream(process.getInputStream(), inputSB)).start();
        new Thread(() -> copyStream(process.getErrorStream(), errorSB)).start();

        try {
            boolean s = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            if (!s) {
                return new ProcessResult(false, "TIMEOUT");
            }
            String result = String.format("[INPUT]: %s;[ERROR]: %s", inputSB.toString(), errorSB.toString());
            log.debug("[ScriptProcessor] process result for instance(instanceId={}) is {}.", instanceId, result);
            return new ProcessResult(true, result);
        }catch (InterruptedException ie) {
            return new ProcessResult(false, "Interrupted");
        }
    }

    private void copyStream(InputStream is, StringBuilder sb) {
        String line;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            log.warn("[ScriptProcessor] copyStream failed.", e);
            sb.append("Exception: ").append(e);
        }
    }

    /**
     * 生成脚本名称
     * @param instanceId 任务实例ID，作为文件名称（使用JobId会有更改不生效的问题）
     * @return 文件名称
     */
    protected abstract String genScriptName(Long instanceId);

    /**
     * 获取运行命令（eg，shell返回 /bin/sh）
     * @return 执行脚本的命令
     */
    protected abstract String fetchRunCommand();
}
