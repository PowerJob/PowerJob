package tech.powerjob.remote.framework.engine;

import java.io.IOException;

/**
 * RemoteEngine
 *
 * @author tjq
 * @since 2022/12/31
 */
public interface RemoteEngine {

    EngineOutput start(EngineConfig engineConfig);

    void close() throws IOException;
}
