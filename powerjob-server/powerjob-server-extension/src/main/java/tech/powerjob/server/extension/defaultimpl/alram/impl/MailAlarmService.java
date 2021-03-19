package tech.powerjob.server.extension.defaultimpl.alram.impl;

import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.extension.defaultimpl.alram.module.Alarm;
import tech.powerjob.server.extension.Alarmable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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

    private String from;
    private static final String FROM_KEY = "spring.mail.username";

    @Override
    public void onFailed(Alarm alarm, List<UserInfoDO> targetUserList) {
        initFrom();
        if (CollectionUtils.isEmpty(targetUserList) || javaMailSender == null || StringUtils.isEmpty(from)) {
            return;
        }

        SimpleMailMessage sm = new SimpleMailMessage();
        try {
            sm.setFrom(from);
            sm.setTo(targetUserList.stream().map(UserInfoDO::getEmail).filter(Objects::nonNull).toArray(String[]::new));
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

    // 不能直接使用 @Value 注入，不存在的时候会报错
    private void initFrom() {
        if (StringUtils.isEmpty(from)) {
            from = environment.getProperty(FROM_KEY);
        }
    }
}
