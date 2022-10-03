-- Upgrade SQL FROM 4.0.x to 4.1.x
-- ----------------------------
-- Table change for workflow_instance_info
-- ----------------------------
alter table workflow_instance_info
    add parent_wf_instance_id bigint default null null comment '上层工作流实例ID';
-- ----------------------------
-- Table change for job_info
-- ----------------------------
alter table job_info add alarm_config varchar(512) comment '告警配置' default null;
