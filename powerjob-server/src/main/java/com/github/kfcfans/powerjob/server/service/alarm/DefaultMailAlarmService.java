package com.github.kfcfans.powerjob.server.service.alarm;

import com.github.kfcfans.powerjob.server.persistence.core.model.UserInfoDO;
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

/**
 * 邮件通知服务
 *
 * @author tjq
 * @since 2020/4/30
 */
@Slf4j
@Service("omsDefaultMailAlarmService")
public class DefaultMailAlarmService implements Alarmable {

    @Resource
    private Environment environment;

    private JavaMailSender javaMailSender;

    private String from;
    private static final String FROM_KEY = "spring.mail.username";

    private static final String MAIL_TITLE = "PowerJob AlarmService";
    private static final String JOB_INSTANCE_FAILED_CONTENT_PATTERN = "Job run failed, detail is: %s";
    private static final String WF_INSTANCE_FAILED_CONTENT_PATTERN = "Workflow run failed, detail is: %s";

    @Override
    public void onJobInstanceFailed(JobInstanceAlarmContent content, List<UserInfoDO> targetUserList) {
        String msg = String.format(JOB_INSTANCE_FAILED_CONTENT_PATTERN, content.fetchContent());
        sendMail(msg, targetUserList);
    }

    @Override
    public void onWorkflowInstanceFailed(WorkflowInstanceAlarmContent content, List<UserInfoDO> targetUserList) {
        String msg = String.format(WF_INSTANCE_FAILED_CONTENT_PATTERN, content.fetchContent());
        sendMail(msg, targetUserList);
    }

    private void sendMail(String msg, List<UserInfoDO> targetUserList) {

        initFrom();
        log.debug("[OmsMailAlarmService] msg: {}, to: {}", msg, targetUserList);
        if (CollectionUtils.isEmpty(targetUserList) || javaMailSender == null || StringUtils.isEmpty(from)) {
            return;
        }

        SimpleMailMessage sm = new SimpleMailMessage();
        try {
            sm.setFrom(from);
            sm.setTo(targetUserList.stream().map(UserInfoDO::getEmail).toArray(String[]::new));
            sm.setSubject(MAIL_TITLE);
            sm.setText(msg);

            javaMailSender.send(sm);
        }catch (Exception e) {
            log.error("[OmsMailAlarmService] send mail({}) failed, reason is {}", sm, e.getMessage());
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
