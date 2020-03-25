package com.github.kfcfans.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Pattens.ask 的响应
 *
 * @author tjq
 * @since 2020/3/18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AskResponse implements Serializable {
    private boolean success;
}
