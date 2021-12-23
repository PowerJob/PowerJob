-- ----------------------------
-- Table change for workflow_instance_info
-- ----------------------------
alter table sx_workflow_instance_info
    add parent_wf_instance_id bigint default null null comment '上层工作流实例ID';
