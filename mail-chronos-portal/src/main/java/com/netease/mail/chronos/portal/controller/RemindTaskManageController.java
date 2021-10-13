package com.netease.mail.chronos.portal.controller;

import com.netease.mail.chronos.base.response.BaseResponse;
import com.netease.mail.chronos.portal.param.RemindTask;
import com.netease.mail.chronos.portal.service.SpRemindTaskManageService;
import com.netease.mail.chronos.portal.vo.RemindTaskVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Echo009
 * @since 2021/9/18
 * <p>
 * only for 提醒任务
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/manage/remind_task")
public class RemindTaskManageController {


    private final SpRemindTaskManageService spRemindTaskManageService;

    /**
     * 创建任务
     */
    @PostMapping
    public BaseResponse<List<RemindTaskVo>> create(@RequestBody RemindTask task) {
        return BaseResponse.success(spRemindTaskManageService.create(task));
    }

    /**
     * 删除 任务
     */
    @DeleteMapping
    public BaseResponse<List<RemindTaskVo>> delete(@RequestParam String colId, @RequestParam(required = false) String compId) {
        return BaseResponse.success(spRemindTaskManageService.delete(colId, compId));
    }

    /**
     * 更新任务
     */
    @PatchMapping
    public BaseResponse<List<RemindTaskVo>> update(@RequestBody RemindTask task) {
        return BaseResponse.success(spRemindTaskManageService.update(task));
    }

    /**
     * 查询任务信息
     */
    @GetMapping
    public BaseResponse<List<RemindTaskVo>> query(@RequestParam String colId, @RequestParam(required = false) String compId) {
        return BaseResponse.success(spRemindTaskManageService.query(colId, compId));
    }


}
