-- Upgrade SQL FROM 4.3.7 to 4.3.8
-- ----------------------------
-- Table change for job_info
-- ----------------------------
alter table job_info add dispatch_strategy_config varchar(255) comment 'dispatch_strategy_config' default null;
alter table job_info add advanced_runtime_config varchar(255) comment 'advanced_runtime_config' default null;
