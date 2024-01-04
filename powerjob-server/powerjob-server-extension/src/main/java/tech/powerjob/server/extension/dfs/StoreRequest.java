package tech.powerjob.server.extension.dfs;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.Serializable;

/**
 * StoreRequest
 *
 * @author tjq
 * @since 2023/7/16
 */
@Data
@Accessors(chain = true)
public class StoreRequest implements Serializable {

    private transient File localFile;

    private FileLocation fileLocation;
}
