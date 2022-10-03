-- Upgrade SQL FROM 4.1.x to 4.2.x
-- ----------------------------
-- Table change for job_info
-- ----------------------------
alter table job_info add tag varchar(255) comment 'TAG' default null;
alter table job_info add log_config varchar(255) comment 'logConfig' default null;
