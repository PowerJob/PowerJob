package tech.powerjob.official.processors.impl.script;

import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.log.OmsLogger;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
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
 * @author Jiang Jining
 * @since 2020/4/16
 */
@Slf4j
public abstract class AbstractScriptProcessor extends CommonBasicProcessor {

    private static final ForkJoinPool POOL = new ForkJoinPool(4 * Runtime.getRuntime().availableProcessors());
    private static final Set<String> DOWNLOAD_PROTOCOL = Sets.newHashSet("http", "https", "ftp");
    protected static final String SH_SHELL = "/bin/sh";

    private static final String WORKER_DIR = System.getProperty("user.home") + "/powerjob/worker/official_script_processor/";

    @Override
    protected ProcessResult process0(TaskContext context) throws Exception {
        OmsLogger omsLogger = context.getOmsLogger();
        String scriptParams = CommonUtils.parseParams(context);
        omsLogger.info("[SYSTEM] ScriptProcessor start to process, params: {}", scriptParams);
        if (scriptParams == null) {
            String message = "[SYSTEM] ScriptParams is null, please check jobParam configuration.";
            omsLogger.warn(message);
            return new ProcessResult(false, message);
        }
        String scriptPath = prepareScriptFile(context.getInstanceId(), scriptParams);
        omsLogger.info("[SYSTEM] Generate executable file successfully, path: {}", scriptPath);
        
        if (SystemUtils.IS_OS_WINDOWS) {
            if (StringUtils.equals(getRunCommand(), SH_SHELL)) {
                String message = String.format("[SYSTEM] Current OS is %s where shell scripts cannot run.", SystemUtils.OS_NAME);
                omsLogger.warn(message);
                return new ProcessResult(false, message);
            }
        }

        // 授权
        ProcessBuilder chmodPb = new ProcessBuilder("/bin/chmod", "755", scriptPath);
        // 等待返回，这里不可能导致死锁（shell产生大量数据可能导致死锁）
        chmodPb.start().waitFor();
        omsLogger.info("[SYSTEM] chmod 755 authorization complete, ready to start execution~");

        // 2. 执行目标脚本
        ProcessBuilder pb = new ProcessBuilder(getRunCommand(), scriptPath);
        Process process = pb.start();

        StringBuilder inputBuilder = new StringBuilder();
        StringBuilder errorBuilder = new StringBuilder();

        boolean success = true;
        String result;

        try (InputStream is = process.getInputStream(); InputStream es = process.getErrorStream()) {

            POOL.execute(() -> copyStream(is, inputBuilder, omsLogger));
            POOL.execute(() -> copyStream(es, errorBuilder, omsLogger));

            success = process.waitFor() == 0;

        } catch (InterruptedException ie) {
            omsLogger.info("[SYSTEM] ScriptProcessor has been interrupted");
        } finally {
            result = String.format("[INPUT]: %s;[ERROR]: %s", inputBuilder.toString(), errorBuilder.toString());
        }
        return new ProcessResult(success, result);
    }

    private String prepareScriptFile(Long instanceId, String processorInfo) throws IOException {
        String scriptPath = WORKER_DIR + getScriptName(instanceId);
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
            omsLogger.warn("[SYSTEM] copyStream failed.", e);

            sb.append("Exception: ").append(e);
        }
    }

    /**
     * 生成脚本名称
     * @param instanceId id of instance
     * @return 文件名称
     */
    protected abstract String getScriptName(Long instanceId);

    /**
     * 获取运行命令（eg，shell返回 /bin/sh）
     * @return 执行脚本的命令
     */
    protected abstract String getRunCommand();
}
