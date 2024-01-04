package tech.powerjob.server.core.alarm.impl;

import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.server.extension.alarm.AlarmTarget;
import tech.powerjob.server.extension.alarm.Alarm;
import tech.powerjob.server.extension.alarm.Alarmable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 邮件通知服务
 *
 * @author tjq
 * @since 2020/4/30
 */
@Slf4j
@Service
public class MailAlarmService implements Alarmable {

    @Resource
    private Environment environment;

    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username:''}")
    private String from;

    @Override
    public void onFailed(Alarm alarm, List<AlarmTarget> targetUserList) {
        if (CollectionUtils.isEmpty(targetUserList) || javaMailSender == null || StringUtils.isEmpty(from)) {
            return;
        }

        SimpleMailMessage sm = new SimpleMailMessage();
        try {
            sm.setFrom(from);
            sm.setTo(targetUserList.stream().map(AlarmTarget::getEmail).filter(Objects::nonNull).toArray(String[]::new));
            sm.setSubject(alarm.fetchTitle());
            sm.setText(alarm.fetchContent());

            javaMailSender.send(sm);
        }catch (Exception e) {
            log.warn("[MailAlarmService] send mail failed, reason is {}", e.getMessage());
        }
    }

    @Autowired(required = false)
    public void setJavaMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

}
