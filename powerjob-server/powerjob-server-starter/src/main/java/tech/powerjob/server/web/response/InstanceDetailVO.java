package tech.powerjob.server.web.response;

import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.model.InstanceDetail;
import tech.powerjob.common.utils.CommonUtils;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 任务实例的运行详细信息（对外展示对象）
 * 注意：日期的格式化全部需要在 server 完成，不能在浏览器完成，否则会有时区问题（当 server 与 browser 时区不一致时显示会有问题）
 *
 * @author tjq
 * @since 2020/7/18
 */
@Data
@NoArgsConstructor
public class InstanceDetailVO {

    /**
     * 任务预计执行时间
     */
    private String expectedTriggerTime;
    /**
     * 任务整体开始时间
     */
    private String actualTriggerTime;
    /**
     * 任务整体结束时间（可能不存在）
     */
    private String finishedTime;
    /**
     * 任务状态
     */
    private Integer status;
    /**
     * 任务执行结果（可能不存在）
     */
    private String result;
    /**
     * TaskTracker地址
     */
    private String taskTrackerAddress;
    /**
     * 任务参数
     */
    private String jobParams;
    /**
     * 启动参数
     */
    private String instanceParams;

    /**
     * MR或BD任务专用
     */
    private InstanceDetailVO.TaskDetail taskDetail;
    /**
     * 秒级任务专用
     */
    private List<InstanceDetailVO.SubInstanceDetail> subInstanceDetails;

    /**
     * 重试次数
     */
    private Long runningTimes;

    /**
     * 秒级任务的 extra -> List<SubInstanceDetail>
     */
    @Data
    @NoArgsConstructor
    public static class SubInstanceDetail implements PowerSerializable {
        private long subInstanceId;
        private String startTime;
        private String finishedTime;
        private String result;
        private int status;
    }

    /**
     * MapReduce 和 Broadcast 任务的 extra ->
     */
    @Data
    @NoArgsConstructor
    public static class TaskDetail implements PowerSerializable {
        private long totalTaskNum;
        private long succeedTaskNum;
        private long failedTaskNum;
    }

    public static InstanceDetailVO from(InstanceDetail origin) {
        InstanceDetailVO vo = new InstanceDetailVO();
        BeanUtils.copyProperties(origin, vo);

        // 格式化时间
        vo.setFinishedTime(CommonUtils.formatTime(origin.getFinishedTime()));
        vo.setActualTriggerTime(CommonUtils.formatTime(origin.getActualTriggerTime()));
        vo.setExpectedTriggerTime(CommonUtils.formatTime(origin.getExpectedTriggerTime()));

        // 拷贝 TaskDetail
        if (origin.getTaskDetail() != null) {
            TaskDetail voDetail = new TaskDetail();
            BeanUtils.copyProperties(origin.getTaskDetail(), voDetail);
            vo.setTaskDetail(voDetail);
        }

        // 拷贝秒级任务数据
        if (!CollectionUtils.isEmpty(origin.getSubInstanceDetails())) {
            vo.subInstanceDetails = Lists.newLinkedList();

            origin.getSubInstanceDetails().forEach(subDetail -> {

                SubInstanceDetail voSubDetail = new SubInstanceDetail();
                BeanUtils.copyProperties(subDetail, voSubDetail);

                // 格式化时间
                voSubDetail.setStartTime(CommonUtils.formatTime(subDetail.getStartTime()));
                voSubDetail.setFinishedTime(CommonUtils.formatTime(subDetail.getFinishedTime()));

                vo.subInstanceDetails.add(voSubDetail);
            });
        }

        return vo;
    }
}
