package tech.powerjob.worker.core.tracker.task.stat;

import lombok.Data;

import java.io.Serializable;
import java.util.concurrent.atomic.LongAdder;

/**
 * 已提交的任务数量
 *
 * @author tjq
 * @since 2024/2/21
 */
@Data
public class CommittedTaskStatistics implements Serializable {

    /**
     * 提交成功的数量
     */
    private LongAdder succeedNum = new LongAdder();
    /**
     * 提交失败的数量
     */
    private LongAdder failedNum = new LongAdder();

    /**
     * 获取全部的提交任务数量
     * @return 提交任务数量
     */
    public long getTotalCommittedNum() {
        return succeedNum.sum() + failedNum.sum();
    }
}
