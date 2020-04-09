package com.github.kfcfans.oms.server.akka.requests;

import lombok.Data;

import java.io.Serializable;

/**
 * 检测目标机器是否存活
 *
 * @author tjq
 * @since 2020/4/5
 */
@Data
public class Ping implements Serializable {
    private long currentTime;
}
