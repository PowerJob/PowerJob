package com.github.kfcfans.powerjob.server.akka.requests;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import lombok.Data;


/**
 * 检测目标机器是否存活
 *
 * @author tjq
 * @since 2020/4/5
 */
@Data
public class Ping implements OmsSerializable {
    private long currentTime;
}
