package tech.powerjob.worker.core.processor;

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

    public ProcessResult(boolean success) {
        this.success = success;
    }
}
