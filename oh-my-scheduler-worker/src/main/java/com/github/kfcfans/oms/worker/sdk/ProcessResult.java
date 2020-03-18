package com.github.kfcfans.oms.worker.sdk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * processor执行结果
 *
 * @author tjq
 * @since 2020/3/18
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResult {

    private boolean success = false;
    private String msg;

}
