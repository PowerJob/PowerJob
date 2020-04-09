package com.github.kfcfans.oms.server.akka.requests;

import com.github.kfcfans.common.request.ServerStopInstanceReq;
import lombok.Data;

import java.io.Serializable;

/**
 * 重定向 ServerStopInstanceReq
 * 被HTTP请求停止任务实例的机器需要将请求转发到该实例对应的Server上处理，由该Server下发到Worker（只有该Server持有Worker的地址信息）
 *
 * @author tjq
 * @since 2020/4/9
 */
@Data
public class RedirectServerStopInstanceReq implements Serializable {
    private ServerStopInstanceReq serverStopInstanceReq;
}
