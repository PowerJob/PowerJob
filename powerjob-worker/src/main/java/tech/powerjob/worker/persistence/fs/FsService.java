package tech.powerjob.worker.persistence.fs;

import java.io.Closeable;
import java.io.IOException;

/**
 * FileSystemService
 *
 * @author tjq
 * @since 2024/2/22
 */
public interface FsService extends Closeable {

    void writeLine(String content) throws IOException;

    String readLine() throws IOException;
}
