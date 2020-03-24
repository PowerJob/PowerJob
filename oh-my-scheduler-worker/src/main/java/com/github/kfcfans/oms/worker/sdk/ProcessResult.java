package com.github.kfcfans.oms.worker.sdk;

import lombok.*;

/**
 * processor执行结果
 *
 * @author tjq
 * @since 2020/3/18
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResult {

    private boolean success = false;
    private String msg;

}
