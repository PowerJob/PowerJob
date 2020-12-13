package com.github.kfcfans.powerjob.server.akka.requests;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 原创执行命令
 *
 * @author tjq
 * @since 12/13/20
 */
@Getter
@Setter
@Accessors(chain = true)
public class RemoteProcessReq implements OmsSerializable {

    private String className;
    private String methodName;
    private String[] parameterTypes;

    private Object[] args;

}
