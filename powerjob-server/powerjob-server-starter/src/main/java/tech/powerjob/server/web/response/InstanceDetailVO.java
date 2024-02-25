package tech.powerjob.server.web.response;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.model.InstanceDetail;
import tech.powerjob.common.utils.CommonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
     * 事实上为 instance 的 task 统计信息，命名为 instanceTaskStats 更合理，不过出于兼容性暂时不改名称了
     */
    private InstanceDetailVO.InstanceTaskStats taskDetail;
    /**
     * 查询出来的 Task 详细结果
     */
    private List<TaskDetailInfoVO> queriedTaskDetailInfoList;
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
    public static class InstanceTaskStats implements PowerSerializable {
        private long totalTaskNum;
        private long succeedTaskNum;
        private long failedTaskNum;

        // 等待派发状态（仅存在 TaskTracker 数据库中）
        protected Long waitingDispatchTaskNum;
        // 已派发，但 ProcessorTracker 未确认，可能由于网络错误请求未送达，也有可能 ProcessorTracker 线程池满，拒绝执行
        protected Long workerUnreceivedTaskNum;
        // ProcessorTracker确认接收，存在与线程池队列中，排队执行
        protected Long receivedTaskNum;
        // ProcessorTracker正在执行
        protected Long runningTaskNum;
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
            InstanceTaskStats voDetail = new InstanceTaskStats();
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

        // 拷贝 MR Task 结果
        List<TaskDetailInfoVO> taskDetailInfoVOList = Optional.ofNullable(origin.getQueriedTaskDetailInfoList()).orElse(Collections.emptyList()).stream().map(TaskDetailInfoVO::from).collect(Collectors.toList());
        vo.setQueriedTaskDetailInfoList(taskDetailInfoVOList);

        return vo;
    }
}
