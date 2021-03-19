package tech.powerjob.common.model;

import tech.powerjob.common.PowerSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Log instance model.
 *
 * @author tjq
 * @since 2020/4/21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceLogContent implements PowerSerializable {

    /**
     * Id of instance.
     */
    private long instanceId;
    /**
     * Submitted time of the log.
     */
    private long logTime;
    /**
     * Level of the log.
     */
    private int logLevel;
    /**
     * Content of the log.
     */
    private String logContent;
}

