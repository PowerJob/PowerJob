package tech.powerjob.server.extension.dfs;

import lombok.Data;

import java.io.File;
import java.io.Serializable;

/**
 * download request
 *
 * @author tjq
 * @since 2023/7/16
 */
@Data
public class DownloadRequest implements Serializable {

    private transient File target;

    private FileLocation fileLocation;
}
