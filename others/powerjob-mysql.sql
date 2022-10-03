SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for app_info
-- ----------------------------
DROP TABLE IF EXISTS `app_info`;
CREATE TABLE `app_info`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '应用ID',
    `app_name`       varchar(128) not NULL COMMENT '应用名称',
    `current_server` varchar(255) default null COMMENT 'Server地址,用于负责调度应用的ActorSystem地址',
    `gmt_create`     datetime     not null COMMENT '创建时间',
    `gmt_modified`   datetime     not null COMMENT '更新时间',
    `password`       varchar(255) not null COMMENT '应用密码',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uidx01_app_info` (`app_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='应用表';

-- ----------------------------
-- Table structure for container_info
-- ----------------------------

DROP TABLE IF EXISTS `container_info`;
CREATE TABLE `container_info`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '容器ID',
    `app_id`           bigint       not null COMMENT '应用ID',
    `container_name`   varchar(128) not null COMMENT '容器名称',
    `gmt_create`       datetime     not null COMMENT '创建时间',
    `gmt_modified`     datetime     not null COMMENT '更新时间',
    `last_deploy_time` datetime     DEFAULT NULL COMMENT '上次部署时间',
    `source_info`      varchar(255) DEFAULT NULL COMMENT '资源信息,内容取决于source_type\n1、FatJar -> String\n2、Git -> JSON，{"repo”:””仓库,”branch”:”分支”,”username”:”账号,”password”:”密码”}',
    `source_type`      int          not null COMMENT '资源类型,1:FatJar/2:Git',
    `status`           int          not null COMMENT '状态,1:正常ENABLE/2:已禁用DISABLE/99:已删除DELETED',
    `version`          varchar(255) default null COMMENT '版本',
    PRIMARY KEY (`id`),
    KEY `idx01_container_info` (`app_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='容器表';

-- ----------------------------
-- Table structure for instance_info
-- ----------------------------
DROP TABLE IF EXISTS `instance_info`;
CREATE TABLE `instance_info`
(
    `id`                    bigint   NOT NULL AUTO_INCREMENT COMMENT '任务实例ID',
    `app_id`                bigint   not null COMMENT '应用ID',
    `instance_id`           bigint   not null COMMENT '任务实例ID',
    `type`                  int      not NULL COMMENT '任务实例类型,1:普通NORMAL/2:工作流WORKFLOW',
    `job_id`                bigint   not NULL COMMENT '任务ID',
    `instance_params`       longtext COMMENT '任务动态参数',
    `job_params`            longtext COMMENT '任务静态参数',
    `actual_trigger_time`   bigint       default NULL COMMENT '实际触发时间',
    `expected_trigger_time` bigint       DEFAULT NULL COMMENT '计划触发时间',
    `finished_time`         bigint       DEFAULT NULL COMMENT '执行结束时间',
    `last_report_time`      bigint       DEFAULT NULL COMMENT '最后上报时间',
    `result`                longtext COMMENT '执行结果',
    `running_times`         bigint       DEFAULT NULL COMMENT '总执行次数,用于重试判断',
    `status`                int      not NULL COMMENT '任务状态,1:等待派发WAITING_DISPATCH/2:等待Worker接收WAITING_WORKER_RECEIVE/3:运行中RUNNING/4:失败FAILED/5:成功SUCCEED/9:取消CANCELED/10:手动停止STOPPED',
    `task_tracker_address`  varchar(255) DEFAULT NULL COMMENT 'TaskTracker地址',
    `wf_instance_id`        bigint       DEFAULT NULL COMMENT '工作流实例ID',
    `additional_data`       longtext comment '附加信息 (JSON)',
    `gmt_create`            datetime not NULL COMMENT '创建时间',
    `gmt_modified`          datetime not NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx01_instance_info` (`job_id`),
    KEY `idx02_instance_info` (`app_id`),
    KEY `idx03_instance_info` (`instance_id`),
    KEY `idx04_instance_info` (`wf_instance_id`),
    KEY `idx05_instance_info` (`expected_trigger_time`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='任务实例表';

-- ----------------------------
-- Table structure for job_info
-- ----------------------------
DROP TABLE IF EXISTS `job_info`;
CREATE TABLE `job_info`
(
    `id`                   bigint   NOT NULL AUTO_INCREMENT,
    `app_id`               bigint            DEFAULT NULL COMMENT '应用ID',
    `job_name`             varchar(128)      DEFAULT NULL COMMENT '任务名称',
    `job_description`      varchar(255)      DEFAULT NULL COMMENT '任务描述',
    `job_params`           text COMMENT '任务默认参数',
    `concurrency`          int               DEFAULT NULL COMMENT '并发度,同时执行某个任务的最大线程数量',
    `designated_workers`   varchar(255)      DEFAULT NULL COMMENT '运行节点,空:不限(多值逗号分割)',
    `dispatch_strategy`    int               DEFAULT NULL COMMENT '投递策略,1:健康优先/2:随机',
    `execute_type`         int      not NULL COMMENT '执行类型,1:单机STANDALONE/2:广播BROADCAST/3:MAP_REDUCE/4:MAP',
    `instance_retry_num`   int      not null DEFAULT 0 COMMENT 'Instance重试次数',
    `instance_time_limit`  bigint   not null DEFAULT 0 COMMENT '任务整体超时时间',
    `lifecycle`            varchar(255)      DEFAULT NULL COMMENT '生命周期',
    `max_instance_num`     int      not null DEFAULT 1 COMMENT '最大同时运行任务数,默认 1',
    `max_worker_count`     int      not null DEFAULT 0 COMMENT '最大运行节点数量',
    `min_cpu_cores`        double   NOT NULL default 0 COMMENT '最低CPU核心数量,0:不限',
    `min_disk_space`       double   NOT NULL default 0 COMMENT '最低磁盘空间(GB),0:不限',
    `min_memory_space`     double   NOT NULL default 0 COMMENT '最低内存空间(GB),0:不限',
    `next_trigger_time`    bigint            DEFAULT NULL COMMENT '下一次调度时间',
    `notify_user_ids`      varchar(255)      DEFAULT NULL COMMENT '报警用户(多值逗号分割)',
    `processor_info`       varchar(255)      DEFAULT NULL COMMENT '执行器信息',
    `processor_type`       int      not NULL COMMENT '执行器类型,1:内建处理器BUILT_IN/2:SHELL/3:PYTHON/4:外部处理器（动态加载）EXTERNAL',
    `status`               int      not NULL COMMENT '状态,1:正常ENABLE/2:已禁用DISABLE/99:已删除DELETED',
    `task_retry_num`       int      not NULL default 0 COMMENT 'Task重试次数',
    `time_expression`      varchar(255)      default NULL COMMENT '时间表达式,内容取决于time_expression_type,1:CRON/2:NULL/3:LONG/4:LONG',
    `time_expression_type` int      not NULL COMMENT '时间表达式类型,1:CRON/2:API/3:FIX_RATE/4:FIX_DELAY,5:WORKFLOW\n）',
    `tag`      varchar(255)      DEFAULT NULL COMMENT 'TAG',
    `log_config`      varchar(255)      DEFAULT NULL COMMENT '日志配置',
    `extra`                varchar(255)      DEFAULT NULL COMMENT '扩展字段',
    `gmt_create`           datetime not NULL COMMENT '创建时间',
    `gmt_modified`         datetime not NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx01_job_info` (`app_id`),
    KEY `idx02_job_info` (`job_name`),
    KEY `idx03_job_info` (`next_trigger_time`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='任务表';

-- ----------------------------
-- Table structure for oms_lock
-- ----------------------------
DROP TABLE IF EXISTS `oms_lock`;
CREATE TABLE `oms_lock`
(
    `id`            bigint   NOT NULL AUTO_INCREMENT COMMENT '序号ID',
    `lock_name`     varchar(128) DEFAULT NULL COMMENT '名称',
    `max_lock_time` bigint       DEFAULT NULL COMMENT '最长持锁时间',
    `ownerip`       varchar(255) DEFAULT NULL COMMENT '拥有者IP',
    `gmt_create`    datetime not NULL COMMENT '创建时间',
    `gmt_modified`  datetime not NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uidx01_oms_lock` (`lock_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='数据库锁';

-- ----------------------------
-- Table structure for server_info
-- ----------------------------
DROP TABLE IF EXISTS `server_info`;
CREATE TABLE `server_info`
(
    `id`           bigint NOT NULL AUTO_INCREMENT COMMENT '服务器ID',
    `gmt_create`   datetime     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified` datetime     DEFAULT NULL COMMENT '更新时间',
    `ip`           varchar(128) DEFAULT NULL COMMENT '服务器IP地址',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uidx01_server_info` (`ip`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='服务器表';

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`     varchar(128) not NULL COMMENT '用户名',
    `password`     varchar(255) default NULL COMMENT '密码',
    `phone`        varchar(255) DEFAULT NULL COMMENT '手机号',
    `email`        varchar(128) not NULL COMMENT '邮箱',
    `extra`        varchar(255) DEFAULT NULL COMMENT '扩展字段',
    `web_hook`     varchar(255) DEFAULT NULL COMMENT 'webhook地址',
    `gmt_create`   datetime     not NULL COMMENT '创建时间',
    `gmt_modified` datetime     not NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    unique index uidx01_user_info (username),
    unique index uidx02_user_info (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='用户表';

-- ----------------------------
-- Table structure for workflow_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_info`;
CREATE TABLE `workflow_info`
(
    `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '工作流ID',
    `app_id`               bigint       not NULL COMMENT '应用ID',
    `wf_name`              varchar(128) not NULL COMMENT '工作流名称',
    `wf_description`       varchar(255)          default NULL COMMENT '工作流描述',
    `extra`                varchar(255)          DEFAULT NULL COMMENT '扩展字段',
    `lifecycle`            varchar(255)          DEFAULT NULL COMMENT '生命周期',
    `max_wf_instance_num`  int          not null DEFAULT 1 COMMENT '最大运行工作流数量,默认 1',
    `next_trigger_time`    bigint                DEFAULT NULL COMMENT '下次调度时间',
    `notify_user_ids`      varchar(255)          DEFAULT NULL COMMENT '报警用户(多值逗号分割)',
    `pedag`                text COMMENT 'DAG信息(JSON)',
    `status`               int          not NULL COMMENT '状态,1:正常ENABLE/2:已禁用DISABLE/99:已删除DELETED',
    `time_expression`      varchar(255)          DEFAULT NULL COMMENT '时间表达式,内容取决于time_expression_type,1:CRON/2:NULL/3:LONG/4:LONG',
    `time_expression_type` int          not NULL COMMENT '时间表达式类型,1:CRON/2:API/3:FIX_RATE/4:FIX_DELAY,5:WORKFLOW\n）',
    `gmt_create`           datetime              DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`         datetime              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx01_workflow_info` (`app_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='工作流表';

-- ----------------------------
-- Table structure for workflow_instance_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_instance_info`;
CREATE TABLE `workflow_instance_info`
(
    `id`                    bigint NOT NULL AUTO_INCREMENT COMMENT '工作流实例ID',
    `wf_instance_id`        bigint   DEFAULT NULL COMMENT '工作流实例ID',
    `workflow_id`           bigint   DEFAULT NULL COMMENT '工作流ID',
    `actual_trigger_time`   bigint   DEFAULT NULL COMMENT '实际触发时间',
    `app_id`                bigint   DEFAULT NULL COMMENT '应用ID',
    `dag`                   text COMMENT 'DAG信息(JSON)',
    `expected_trigger_time` bigint   DEFAULT NULL COMMENT '计划触发时间',
    `finished_time`         bigint   DEFAULT NULL COMMENT '执行结束时间',
    `result`                text COMMENT '执行结果',
    `status`                int      DEFAULT NULL COMMENT '工作流实例状态,1:等待调度WAITING/2:运行中RUNNING/3:失败FAILED/4:成功SUCCEED/10:手动停止STOPPED',
    `wf_context`            text COMMENT '工作流上下文',
    `wf_init_params`        text COMMENT '工作流启动参数',
    `gmt_create`            datetime DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`          datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    unique index uidx01_wf_instance (wf_instance_id),
    index idx01_wf_instance (workflow_id),
    index idx02_wf_instance (app_id, status)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='工作流实例表';

-- ----------------------------
-- Table structure for workflow_node_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_node_info`;
CREATE TABLE `workflow_node_info`
(
    `id`               bigint   NOT NULL AUTO_INCREMENT COMMENT '节点ID',
    `app_id`           bigint   NOT NULL COMMENT '应用ID',
    `enable`           bit(1)   NOT NULL COMMENT '是否启动,0:否/1:是',
    `extra`            text COMMENT '扩展字段',
    `gmt_create`       datetime NOT NULL COMMENT '创建时间',
    `gmt_modified`     datetime NOT NULL COMMENT '更新时间',
    `job_id`           bigint       default NULL COMMENT '任务ID',
    `node_name`        varchar(255) DEFAULT NULL COMMENT '节点名称',
    `node_params`      text COMMENT '节点参数',
    `skip_when_failed` bit(1)   NOT NULL COMMENT '是否允许失败跳过,0:否/1:是',
    `type`             int          DEFAULT NULL COMMENT '节点类型,1:任务JOB',
    `workflow_id`      bigint       DEFAULT NULL COMMENT '工作流ID',
    PRIMARY KEY (`id`),
    KEY `idx01_workflow_node_info` (`app_id`),
    KEY `idx02_workflow_node_info` (`workflow_id`),
    KEY `idx03_workflow_node_info` (`job_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='工作流节点表';

SET FOREIGN_KEY_CHECKS = 1;
