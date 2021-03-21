package tech.powerjob.official.processors.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import tech.powerjob.worker.core.processor.TaskContext;
import org.junit.jupiter.api.Test;
import tech.powerjob.official.processors.TestUtils;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * test FileCleanupProcessor
 *
 * @author tjq
 * @since 2021/2/1
 */
class FileCleanupProcessorTest {

    @Test
    void testPatternCompile() throws Exception {
        String fileName = "abc.log";
        System.out.println(fileName.matches("[\\s\\S]*log"));
        System.out.println(Pattern.matches("[a-z.0-9]*log", fileName));
    }

    @Test
    void testScriptCompile() throws Exception {
        Pattern compile = Pattern.compile("(shell|python)_[0-9]*\\.(sh|py)");
        String fileNameA = "shell_158671537124147264.sh";
        String fileNameB = "python_158671537124147264.py";
        assertTrue(compile.matcher(fileNameA).matches());
        assertTrue(compile.matcher(fileNameB).matches());
    }

    @Test
    void testProcess() throws Exception {
        JSONObject params = new JSONObject();
        params.put("dirPath", "/Users/tjq/logs");
        params.put("filePattern", "[\\s\\S]*log");
        params.put("retentionTime", 0);
        JSONArray array = new JSONArray();
        array.add(params);

        String paramsStr = array.toJSONString();
        System.out.println(paramsStr);

        TaskContext taskContext = TestUtils.genTaskContext(paramsStr);
        System.out.println(new FileCleanupProcessor().process(taskContext));
    }

    @Test
    void testCleanWorkerScript() throws Exception {
        JSONObject params = new JSONObject();
        params.put("dirPath", "/");
        params.put("filePattern", "(shell|python)_[0-9]*\\.(sh|py)");
        params.put("retentionTime", 24);
        JSONArray array = new JSONArray();
        array.add(params);

        TaskContext taskContext = TestUtils.genTaskContext(array.toJSONString());
        System.out.println(new FileCleanupProcessor().process(taskContext));
    }
}