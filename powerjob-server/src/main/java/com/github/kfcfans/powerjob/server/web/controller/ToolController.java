package com.github.kfcfans.powerjob.server.web.controller;

import com.github.kfcfans.powerjob.common.OmsConstant;
import com.github.kfcfans.powerjob.common.response.ResultDTO;
import com.github.kfcfans.powerjob.server.common.utils.CronExpression;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 工具 Controller
 *
 * @author tjq
 * @since 2020/11/28
 */
@Slf4j
@RestController
@RequestMapping("/tool")
public class ToolController {

    @GetMapping("/validateCron")
    public ResultDTO<List<String>> calculateNextCronTriggerTime(String expression) throws Exception {
        CronExpression cronExpression = new CronExpression(expression);
        List<String> result = Lists.newArrayList();
        Date time = new Date();
        for (int i = 0; i < 10; i++) {
            Date nextValidTime = cronExpression.getNextValidTimeAfter(time);
            if (nextValidTime == null) {
                break;
            }
            result.add(DateFormatUtils.format(nextValidTime.getTime(), OmsConstant.TIME_PATTERN));
            time = nextValidTime;
        }
        return ResultDTO.success(result);
    }
}
