package tech.powerjob.server.extension.dfs;

import lombok.Data;

import java.io.File;
import java.io.Serializable;

/**
 * StoreRequest
 *
 * @author tjq
 * @since 2023/7/16
 */
@Data
public class StoreRequest implements Serializable {

    private transient File localFile;

    private FileLocation fileLocation;
}
