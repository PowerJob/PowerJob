package com.github.kfcfans.powerjob.common.model;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The class for deployed container.
 *
 * @author tjq
 * @since 2020/5/18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeployedContainerInfo implements OmsSerializable {

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
     * Address of the server. Report is not required.
     */
    private String workerAddress;
}
