/*
 Navicat MySQL Data Transfer

 Source Server         : Localhost
 Source Server Type    : MySQL
 Source Server Version : 80023
 Source Host           : 127.0.0.1:3306
 Source Schema         : powerjob-daily

 Target Server Type    : MySQL
 Target Server Version : 80023
 File Encoding         : 65001

 Date: 18/04/2021 17:30:23
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for app_info
-- ----------------------------
DROP TABLE IF EXISTS `app_info`;
CREATE TABLE `app_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '应用ID',
  `app_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '应用名称',
  `current_server` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Server地址,用于负责调度应用的ActorSystem地址',
  `gmt_create` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '应用密码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `appNameUK` (`app_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用表';

-- ----------------------------
-- Table structure for container_info
-- ----------------------------
DROP TABLE IF EXISTS `container_info`;
CREATE TABLE `container_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '容器ID',
  `app_id` bigint DEFAULT NULL COMMENT '应用ID',
  `container_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '容器名称',
  `gmt_create` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `last_deploy_time` datetime(6) DEFAULT NULL COMMENT '上次部署时间',
  `source_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '资源信息,内容取决于source_type\n1、FatJar -> String\n2、Git -> JSON，{"repo”:””仓库,”branch”:”分支”,”username”:”账号,”password”:”密码”}',
  `source_type` int DEFAULT NULL COMMENT '资源类型,1:FatJar/2:Git',
  `status` int DEFAULT NULL COMMENT '状态,1:正常ENABLE/2:已禁用DISABLE/99:已删除DELETED',
  `version` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版本',
  PRIMARY KEY (`id`),
  KEY `IDX8hixyaktlnwil2w9up6b0p898` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='容器表';

-- ----------------------------
-- Table structure for instance_info
-- ----------------------------
DROP TABLE IF EXISTS `instance_info`;
CREATE TABLE `instance_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务实例ID',
  `actual_trigger_time` bigint DEFAULT NULL COMMENT '实际触发时间',
  `app_id` bigint DEFAULT NULL COMMENT '应用ID',
  `expected_trigger_time` bigint DEFAULT NULL COMMENT '计划触发时间',
  `finished_time` bigint DEFAULT NULL COMMENT '执行结束时间',
  `gmt_create` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `instance_id` bigint DEFAULT NULL COMMENT '任务实例ID',
  `instance_params` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '任务动态参数',
  `job_id` bigint DEFAULT NULL COMMENT '任务ID',
  `job_params` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '任务静态参数',
  `last_report_time` bigint DEFAULT NULL COMMENT '最后上报时间',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '执行结果',
  `running_times` bigint DEFAULT NULL COMMENT '总执行次数,用于重试判断',
  `status` int DEFAULT NULL COMMENT '任务状态,1:等待派发WAITING_DISPATCH/2:等待Worker接收WAITING_WORKER_RECEIVE/3:运行中RUNNING/4:失败FAILED/5:成功SUCCEED/9:取消CANCELED/10:手动停止STOPPED',
  `task_tracker_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TaskTracker地址',
  `type` int DEFAULT NULL COMMENT '任务实例类型,1:普通NORMAL/2:工作流WORKFLOW',
  `wf_instance_id` bigint DEFAULT NULL COMMENT '工作流实例ID',
  PRIMARY KEY (`id`),
  KEY `IDX5b1nhpe5je7gc5s1ur200njr7` (`job_id`),
  KEY `IDXjnji5lrr195kswk6f7mfhinrs` (`app_id`),
  KEY `IDXa98hq3yu0l863wuotdjl7noum` (`instance_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务实例表';

-- ----------------------------
-- Table structure for job_info
-- ----------------------------
DROP TABLE IF EXISTS `job_info`;
CREATE TABLE `job_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` bigint DEFAULT NULL COMMENT '应用ID',
  `concurrency` int DEFAULT NULL COMMENT '并发度,同时执行某个任务的最大线程数量',
  `designated_workers` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '运行节点,空:不限(多值逗号分割)',
  `dispatch_strategy` int DEFAULT NULL COMMENT '投递策略,1:健康优先/2:随机',
  `execute_type` int DEFAULT NULL COMMENT '执行类型,1:单机STANDALONE/2:广播BROADCAST/3:MAP_REDUCE/4:MAP',
  `extra` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扩展字段',
  `gmt_create` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `instance_retry_num` int DEFAULT NULL COMMENT 'Instance重试次数',
  `instance_time_limit` bigint DEFAULT NULL COMMENT '任务整体超时时间',
  `job_description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务描述',
  `job_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务名称',
  `job_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '任务默认参数',
  `lifecycle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生命周期',
  `max_instance_num` int DEFAULT '1' COMMENT '最大同时运行任务数,默认 1',
  `max_worker_count` int DEFAULT NULL COMMENT '最大运行节点数量',
  `min_cpu_cores` double NOT NULL COMMENT '最低CPU核心数量,0:不限',
  `min_disk_space` double NOT NULL COMMENT '最低磁盘空间(GB),0:不限',
  `min_memory_space` double NOT NULL COMMENT '最低内存空间(GB),0:不限',
  `next_trigger_time` bigint DEFAULT NULL COMMENT '下一次调度时间',
  `notify_user_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '报警用户(多值逗号分割)',
  `processor_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行器信息',
  `processor_type` int DEFAULT NULL COMMENT '执行器类型,1:内建处理器BUILT_IN/2:SHELL/3:PYTHON/4:外部处理器（动态加载）EXTERNAL',
  `status` int DEFAULT NULL COMMENT '状态,1:正常ENABLE/2:已禁用DISABLE/99:已删除DELETED',
  `task_retry_num` int DEFAULT NULL COMMENT 'Task重试次数',
  `time_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '时间表达式,内容取决于time_expression_type,1:CRON/2:NULL/3:LONG/4:LONG',
  `time_expression_type` int DEFAULT NULL COMMENT '时间表达式类型,1:CRON/2:API/3:FIX_RATE/4:FIX_DELAY,5:WORKFLOW\n）',
  PRIMARY KEY (`id`),
  KEY `IDXk2xprmn3lldmlcb52i36udll1` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务表';

-- ----------------------------
-- Table structure for oms_lock
-- ----------------------------
DROP TABLE IF EXISTS `oms_lock`;
CREATE TABLE `oms_lock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '序号ID',
  `gmt_create` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `lock_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `max_lock_time` bigint DEFAULT NULL COMMENT '最长持锁时间',
  `ownerip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '拥有者IP',
  PRIMARY KEY (`id`),
  UNIQUE KEY `lockNameUK` (`lock_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据库锁';

-- ----------------------------
-- Table structure for server_info
-- ----------------------------
DROP TABLE IF EXISTS `server_info`;
CREATE TABLE `server_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '服务器ID',
  `gmt_create` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '服务器IP地址',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtk8ytgpl7mpukhnvhbl82kgvy` (`ip`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='服务器表';

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `extra` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扩展字段',
  `gmt_create` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
  `web_hook` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'webhook地址',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ----------------------------
-- Table structure for workflow_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_info`;
CREATE TABLE `workflow_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作流ID',
  `app_id` bigint DEFAULT NULL COMMENT '应用ID',
  `extra` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扩展字段',
  `gmt_create` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `lifecycle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生命周期',
  `max_wf_instance_num` int DEFAULT '1' COMMENT '最大运行工作流数量,默认 1',
  `next_trigger_time` bigint DEFAULT NULL COMMENT '下次调度时间',
  `notify_user_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '报警用户(多值逗号分割)',
  `pedag` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'DAG信息(JSON)',
  `status` int DEFAULT NULL COMMENT '状态,1:正常ENABLE/2:已禁用DISABLE/99:已删除DELETED',
  `time_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '时间表达式,内容取决于time_expression_type,1:CRON/2:NULL/3:LONG/4:LONG',
  `time_expression_type` int DEFAULT NULL COMMENT '时间表达式类型,1:CRON/2:API/3:FIX_RATE/4:FIX_DELAY,5:WORKFLOW\n）',
  `wf_description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工作流描述',
  `wf_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工作流名称',
  PRIMARY KEY (`id`),
  KEY `IDX7uo5w0e3beeho3fnx9t7eiol3` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流表';

-- ----------------------------
-- Table structure for workflow_instance_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_instance_info`;
CREATE TABLE `workflow_instance_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作流实例ID',
  `actual_trigger_time` bigint DEFAULT NULL COMMENT '实际触发事件',
  `app_id` bigint DEFAULT NULL COMMENT '应用ID',
  `dag` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'DAG信息(JSON)',
  `expected_trigger_time` bigint DEFAULT NULL COMMENT '计划触发时间',
  `finished_time` bigint DEFAULT NULL COMMENT '执行结束时间',
  `gmt_create` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '执行结果',
  `status` int DEFAULT NULL COMMENT '工作流实例状态,1:等待调度WAITING/2:运行中RUNNING/3:失败FAILED/4:成功SUCCEED/10:手动停止STOPPED',
  `wf_context` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '工作流上下文',
  `wf_init_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '工作流启动参数',
  `wf_instance_id` bigint DEFAULT NULL COMMENT '工作流实例ID',
  `workflow_id` bigint DEFAULT NULL COMMENT '工作流ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流实例表';

-- ----------------------------
-- Table structure for workflow_node_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_node_info`;
CREATE TABLE `workflow_node_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `enable` bit(1) NOT NULL COMMENT '是否启动,0:否/1:是',
  `extra` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '扩展字段',
  `gmt_create` datetime(6) NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime(6) NOT NULL COMMENT '更新时间',
  `job_id` bigint DEFAULT NULL COMMENT '任务ID',
  `node_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '节点名称',
  `node_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '节点参数',
  `skip_when_failed` bit(1) NOT NULL COMMENT '是否允许失败跳过,0:否/1:是',
  `type` int DEFAULT NULL COMMENT '节点类型,1:任务JOB',
  `workflow_id` bigint DEFAULT NULL COMMENT '工作流ID',
  PRIMARY KEY (`id`),
  KEY `IDX36t7rhj4mkg2a5pb4ttorscta` (`app_id`),
  KEY `IDXacr0i6my8jr002ou8i1gmygju` (`workflow_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作流节点表';

SET FOREIGN_KEY_CHECKS = 1;
