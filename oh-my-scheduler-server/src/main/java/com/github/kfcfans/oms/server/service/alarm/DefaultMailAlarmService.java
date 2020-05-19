package com.github.kfcfans.oms.server.service.alarm;

import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.server.persistence.core.model.UserInfoDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 邮件通知服务
 *
 * @author tjq
 * @since 2020/4/30
 */
@Slf4j
@Service("omsDefaultMailAlarmService")
public class DefaultMailAlarmService implements Alarmable {

    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;

    private static final String MAIL_TITLE = "OhMyScheduler 任务执行失败报警";
    private static final String MAIL_CONTENT_PATTERN = "任务运行失败，详细信息如下：%s";

    @Autowired(required = false)
    public DefaultMailAlarmService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void alarm(AlarmContent alarmContent, List<UserInfoDO> targetUserList) {

        log.debug("[DefaultMailAlarmService] content: {}, user: {}", alarmContent, targetUserList);

        if (CollectionUtils.isEmpty(targetUserList)) {
            return;
        }

        SimpleMailMessage sm = new SimpleMailMessage();
        sm.setFrom(from);
        sm.setTo(targetUserList.stream().map(UserInfoDO::getEmail).toArray(String[]::new));
        sm.setSubject(MAIL_TITLE);
        sm.setText(String.format(MAIL_CONTENT_PATTERN, JsonUtils.toJSONString(alarmContent)));

        try {
            javaMailSender.send(sm);
        }catch (Exception e) {
            log.error("[DefaultMailAlarmService] send mail({}) failed.", sm, e);
        }
    }
}
