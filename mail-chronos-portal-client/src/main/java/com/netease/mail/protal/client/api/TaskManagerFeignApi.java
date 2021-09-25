package com.netease.mail.protal.client.api;

import com.netease.mail.chronos.base.response.BaseResponse;
import com.netease.mail.protal.client.config.PortalAuthConfig;
import com.netease.mail.protal.client.param.RemindTask;
import com.netease.mail.protal.client.vo.RemindTaskVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * @author Echo009
 * @since 2021/9/26
 */
@FeignClient(value = "mail-chronos-portal",configuration = {
        PortalAuthConfig.class
})
public interface TaskManagerFeignApi {


    /**
     * 创建提醒任务（幂等)
     * 如果存在同一个 originId 的任务将会创建失败
     *
     * @param task 任务详情
     * @return 任务信息（含下一次触发时间）
     */
    @PostMapping("/manage/remind_task")
    BaseResponse<RemindTaskVo> create(@RequestBody RemindTask task);


    /**
     * 删除任务（幂等）
     *
     * @param originId 任务原始 ID
     * @return 被删除的任务信息
     */
    @DeleteMapping("/manage/remind_task")
    BaseResponse<RemindTaskVo> delete(@RequestParam String originId);

    /**
     * 更新任务
     *
     * @param task 任务详情
     * @return 更新后的任务信息
     */
    @PatchMapping("/manage/remind_task")
    BaseResponse<RemindTaskVo> update(@RequestBody RemindTask task);

    /**
     * 查询任务信息
     *
     * @param originId 原始任务 ID
     * @return 任务信息
     */
    @GetMapping("/manage/remind_task")
    BaseResponse<RemindTaskVo> get(@RequestParam String originId);


}
