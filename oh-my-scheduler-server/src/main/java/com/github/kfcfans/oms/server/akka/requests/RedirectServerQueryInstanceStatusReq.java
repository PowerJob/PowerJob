package com.github.kfcfans.oms.server.akka.requests;

import com.github.kfcfans.common.request.ServerQueryInstanceStatusReq;
import lombok.Data;

import java.io.Serializable;

/**
 * 重定向 ServerQueryInstanceStatusReq
 *
 * @author tjq
 * @since 2020/4/10
 */
@Data
public class RedirectServerQueryInstanceStatusReq implements Serializable {
    private ServerQueryInstanceStatusReq req;
}
