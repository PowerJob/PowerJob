package com.netease.mail.chronos.portal.client.api;

import com.netease.mail.chronos.base.response.BaseResponse;
import com.netease.mail.chronos.portal.client.config.PortalAuthConfig;
import com.netease.mail.chronos.portal.client.param.RemindTask;
import com.netease.mail.chronos.portal.client.vo.RemindTaskVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Echo009
 * @since 2021/9/18
 */
@FeignClient(value = "chronos-portal", configuration = {
        PortalAuthConfig.class
})
public interface TaskManagerFeignApi {


    /**
     * 创建提醒任务（幂等)
     * 如果存在同一个 colId 和 compId 的任务将会创建失败（返回 code = 1001）
     *
     * @param task 任务详情
     * @return 任务信息（含下一次触发时间）
     */
    @PostMapping("/manage/remind_task")
    BaseResponse<List<RemindTaskVo>> create(@RequestBody RemindTask task);


    /**
     * 删除任务（幂等）
     * compId & colId 不能都为空
     * 当仅传递 colId 时，会删除所有 colId 一致的任务
     *
     * @param colId  colId
     * @param compId compId
     * @return 被删除的任务信息
     */
    @DeleteMapping("/manage/remind_task")
    BaseResponse<List<RemindTaskVo>> delete(@RequestParam(value = "colId",required = false) String colId, @RequestParam(value = "compId", required = false) String compId);

    /**
     * 更新任务 （会先删除再创建）
     *
     * @param task 任务详情
     * @return 更新后的任务信息
     */
    @PatchMapping("/manage/remind_task")
    BaseResponse<List<RemindTaskVo>> update(@RequestBody RemindTask task);

    /**
     * 查询任务信息
     *
     * @param colId  colId
     * @param compId compId
     * @return 任务信息
     */
    @GetMapping("/manage/remind_task")
    BaseResponse<List<RemindTaskVo>> get(@RequestParam(value = "colId",required = false) String colId, @RequestParam(value = "compId", required = false) String compId);


}
