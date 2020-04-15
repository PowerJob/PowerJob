package com.github.kfcfans.oms.worker.core.executor;

import com.github.kfcfans.oms.worker.sdk.ProcessResult;
import com.github.kfcfans.oms.worker.sdk.TaskContext;
import com.github.kfcfans.oms.worker.sdk.api.BasicProcessor;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Set;

/**
 * Shell 处理器
 * 由 ProcessorTracker 创建
 *
 * @author tjq
 * @since 2020/4/15
 */
@Slf4j
public class ShellProcessor implements BasicProcessor {

    private Long instanceId;
    // shell 脚本绝对路径
    private String scriptPath;

    private static final String SHELL_PREFIX = "#!/bin/";
    private static final String DEFAULT_ACTUATOR = "sh";

    private static final String FILE_PATH_PATTERN = "~/.oms/script/shell/%d.sh";

    private static final Set<String> DOWNLOAD_PROTOCOL = Sets.newHashSet("http", "https", "ftp");

    public ShellProcessor(Long instanceId, String processorInfo) throws Exception {

        this.instanceId = instanceId;
        this.scriptPath = String.format(FILE_PATH_PATTERN, instanceId);

        // 如果是下载连接，则从网络获取
        for (String protocol : DOWNLOAD_PROTOCOL) {
            if (processorInfo.startsWith(protocol)) {
                downloadShellScript(processorInfo);
                return;
            }
        }

        // 如是只是单纯的 shell 命令，则将其补充为 shell 脚本
        if (!processorInfo.startsWith(SHELL_PREFIX)) {
            processorInfo = SHELL_PREFIX + DEFAULT_ACTUATOR + System.lineSeparator() + processorInfo;
        }

        // 写入本地文件
        File script = new File(scriptPath);
        if (!script.exists()) {
            File dir = new File(script.getParent());
            boolean success = dir.mkdirs();
            success = script.createNewFile();
            if (!success) {
                throw new RuntimeException("create script file failed");
            }
        }
        try (FileWriter fw = new FileWriter(script); BufferedWriter bw = new BufferedWriter(fw);) {
            bw.write(processorInfo);
            bw.flush();
        }
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        // 1. 授权
        ProcessBuilder chmodPb = new ProcessBuilder("/bin/chmod", "755", scriptPath);
        chmodPb.start().waitFor();

        String chmod = "chmod +x " + scriptPath;
        Process chmodProcess = Runtime.getRuntime().exec(chmod);
        // 等待返回，这里不可能导致死锁（shell产生大量数据可能导致死锁）
        chmodProcess.waitFor();

        // 2. 执行目标脚本
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", scriptPath);
        Process process = pb.start();
        String s;
        StringBuilder inputSB = new StringBuilder();
        StringBuilder errorSB = new StringBuilder();
        try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            while ((s = stdInput.readLine()) != null) {
                inputSB.append(s);
            }
            while ((s = stdError.readLine()) != null) {
                errorSB.append(s);
            }
        }
        process.waitFor();

        String result = null;
        if (inputSB.length() > 0) {
            result = "input:" + inputSB.toString() + " ; ";
        }
        if (errorSB.length() > 0) {
            result  = "error: " + errorSB.toString() + " ; ";
        }
        if (result == null) {
            result = "PROCESS_SUCCESS";
        }

        log.debug("[ShellProcessor] process result for instance(instanceId={}) is {}.", instanceId, result);
        return new ProcessResult(true, result);
    }

    private void downloadShellScript(String url) {
        // 1. 下载
        // 2. 读取前两位，获取解释器
    }
}
