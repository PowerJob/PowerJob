package tech.powerjob.common.model;

import tech.powerjob.common.PowerSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deployed Container Information
 *
 * @author tjq
 * @since 2020/5/18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeployedContainerInfo implements PowerSerializable {

    /**
     * Id of the container.
     */
    private Long containerId;
    /**
     * Version of the container.
     */
    private String version;
    /**
     * Deploy timestamp.
     */
    private long deployedTime;
    /**
     * No need to report to the server
     */
    private String workerAddress;
}
