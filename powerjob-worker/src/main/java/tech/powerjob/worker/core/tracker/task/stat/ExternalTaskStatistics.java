package tech.powerjob.worker.core.tracker.task.stat;

import lombok.Data;

import java.io.Serializable;
import java.util.concurrent.atomic.LongAdder;

/**
 * 外部任务（未持久化到运行时）统计
 *
 * @author tjq
 * @since 2024/2/21
 */
@Data
public class ExternalTaskStatistics implements Serializable {

    /**
     * 等待交换进入的运行时的数量
     */
    private LongAdder waitSwapInNum = new LongAdder();

    /**
     * 运行成功，交换外部的数量
     */
    private LongAdder succeedSwapOutNum = new LongAdder();

    /**
     * 盖棺定论失败，交换到外部的数量
     */
    private LongAdder failedSwapOutNum = new LongAdder();
}
