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

    private static final String MAIL_TITLE = "OhMyScheduler AlarmService";
    private static final String JOB_INSTANCE_FAILED_CONTENT_PATTERN = "Job run failed, detail is: %s";
    private static final String WF_INSTANCE_FAILED_CONTENT_PATTERN = "Workflow run failed, detail is: %s";

    @Autowired(required = false)
    public DefaultMailAlarmService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void onJobInstanceFailed(JobInstanceAlarmContent content, List<UserInfoDO> targetUserList) {
        String msg = String.format(JOB_INSTANCE_FAILED_CONTENT_PATTERN, JsonUtils.toJSONString(content));
        sendMail(msg, targetUserList);
    }

    @Override
    public void onWorkflowInstanceFailed(WorkflowInstanceAlarmContent content, List<UserInfoDO> targetUserList) {
        String msg = String.format(WF_INSTANCE_FAILED_CONTENT_PATTERN, JsonUtils.toJSONString(content));
        sendMail(msg, targetUserList);
    }

    private void sendMail(String msg, List<UserInfoDO> targetUserList) {

        log.debug("[OmsMailAlarmService] msg: {}, to: {}", msg, targetUserList);

        if (CollectionUtils.isEmpty(targetUserList) || javaMailSender == null) {
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
            log.error("[OmsMailAlarmService] send mail({}) failed.", sm, e);
        }
    }
}
