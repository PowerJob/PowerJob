package tech.powerjob.worker.persistence.fs.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.worker.common.utils.PowerFileUtils;
import tech.powerjob.worker.persistence.fs.FsService;

import java.io.*;

/**
 * 本地磁盘
 *
 * @author tjq
 * @since 2024/2/22
 */
@Slf4j
public class LocalDiskFsService implements FsService {

    private static final String WORKSPACE_PATH = PowerFileUtils.workspace() + "/fs/" + CommonUtils.genUUID() + "/";

    private static final String FILE_NAME_PATTERN = "%s.powerjob";

    private final File file;
    private final BufferedWriter bufferedWriter;

    private final BufferedReader bufferedReader;

    @SneakyThrows
    public LocalDiskFsService(String keyword) {
        String fileName = String.format(FILE_NAME_PATTERN, keyword);
        String filePath = WORKSPACE_PATH.concat(fileName);

        this.file = new File(filePath);
        FileUtils.createParentDirectories(file);

        // 在使用 BufferedReader 包装 FileReader 的情况下，不需要单独关闭 FileReader。当你调用 BufferedReader 的 close() 方法时，它会负责关闭它所包装的 FileReader。这是因为 BufferedReader.close() 方法内部会调用它所包装的流的 close() 方法，确保所有相关资源都被释放，包括底层的文件句柄
        FileWriter fileWriter = new FileWriter(file);
        this.bufferedWriter = new BufferedWriter(fileWriter);
        this.bufferedReader = new BufferedReader(new FileReader(file));

        log.info("[LocalDiskFsService] new LocalDiskFsService successfully, path: {}", filePath);
    }

    /**
     * 按行写数据，线程不安全，考虑到此处不用太在意性能，直接 synchronized
     * @param content 内容
     * @throws IOException 异常
     */
    @Override
    public synchronized void writeLine(String content) throws IOException {
        bufferedWriter.write(content);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    /**
     * 按行读数据，线程不安全，考虑到此处不用太在意性能，直接 synchronized
     * @return 内容
     * @throws IOException 异常
     */
    @Override
    public synchronized String readLine() throws IOException {
        return bufferedReader.readLine();
    }

    @Override
    public void close() {

        CommonUtils.executeIgnoreException(() -> {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        });

        CommonUtils.executeIgnoreException(() -> {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        });

        CommonUtils.executeIgnoreException(() -> {
            boolean delete = file.delete();
            log.info("[LocalDiskFsService] delete file[{}] result: {}", file, delete);
        });
    }
}
