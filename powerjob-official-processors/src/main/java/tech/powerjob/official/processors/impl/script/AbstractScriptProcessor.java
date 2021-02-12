package tech.powerjob.official.processors.impl.script;

import com.github.kfcfans.powerjob.worker.common.utils.OmsWorkerFileUtils;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.log.OmsLogger;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import tech.powerjob.official.processors.CommonBasicProcessor;
import tech.powerjob.official.processors.util.CommonUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

/**
 * 脚本处理器
 *
 * @author tjq
 * @since 2020/4/16
 */
@Slf4j
public abstract class AbstractScriptProcessor extends CommonBasicProcessor {

    private static final ForkJoinPool pool = new ForkJoinPool(4 * Runtime.getRuntime().availableProcessors());
    private static final Set<String> DOWNLOAD_PROTOCOL = Sets.newHashSet("http", "https", "ftp");

    @Override
    protected ProcessResult process0(TaskContext context) throws Exception {
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.info("SYSTEM ===> ScriptProcessor start to process");

        String scriptPath = prepareScriptFile(context.getInstanceId(), CommonUtils.parseParams(context));

        // 1. 授权
        ProcessBuilder chmodPb = new ProcessBuilder("/bin/chmod", "755", scriptPath);
        // 等待返回，这里不可能导致死锁（shell产生大量数据可能导致死锁）
        chmodPb.start().waitFor();

        // 2. 执行目标脚本
        ProcessBuilder pb = new ProcessBuilder(getRunCommand(), scriptPath);
        Process process = pb.start();

        StringBuilder inputSB = new StringBuilder();
        StringBuilder errorSB = new StringBuilder();

        pool.execute(() -> copyStream(process.getInputStream(), inputSB, omsLogger));
        pool.execute(() -> copyStream(process.getErrorStream(), errorSB, omsLogger));

        try {
            boolean success = process.waitFor() == 0;
            String result = String.format("[INPUT]: %s;[ERROR]: %s", inputSB.toString(), errorSB.toString());

            return new ProcessResult(success, result);
        }catch (InterruptedException ie) {
            omsLogger.info("SYSTEM ===> ScriptProcessor has been interrupted");
            return new ProcessResult(false, "Interrupted");
        }
    }

    private String prepareScriptFile(Long instanceId, String processorInfo) throws IOException {
        String scriptPath = OmsWorkerFileUtils.getScriptDir() + getScriptName(instanceId);
        File script = new File(scriptPath);
        if (script.exists()) {
            return scriptPath;
        }
        File dir = new File(script.getParent());
        boolean success = dir.mkdirs();
        success = script.createNewFile();
        if (!success) {
            throw new RuntimeException("create script file failed");
        }

        // 如果是下载链接，则从网络获取
        for (String protocol : DOWNLOAD_PROTOCOL) {
            if (processorInfo.startsWith(protocol)) {
                FileUtils.copyURLToFile(new URL(processorInfo), script, 5000, 300000);
                return scriptPath;
            }
        }

        // 非下载链接，为 processInfo 生成可执行文件
        try (FileWriter fw = new FileWriter(script); BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(processorInfo);
            bw.flush();
        }
        return scriptPath;
    }

    private static void copyStream(InputStream is, StringBuilder sb, OmsLogger omsLogger) {
        String line;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
                // 同步到在线日志
                omsLogger.info(line);
            }
        } catch (Exception e) {
            log.warn("[ScriptProcessor] copyStream failed.", e);
            omsLogger.warn("[ScriptProcessor] copyStream failed.", e);

            sb.append("Exception: ").append(e);
        }
    }

    /**
     * 生成脚本名称
     * @return 文件名称
     */
    protected abstract String getScriptName(Long instanceId);

    /**
     * 获取运行命令（eg，shell返回 /bin/sh）
     * @return 执行脚本的命令
     */
    protected abstract String getRunCommand();
}
