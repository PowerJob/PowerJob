/*
官方 SQL 仅基于特定版本（MySQL8）导出，不一定兼容其他数据库，也不一定兼容其他版本。此 SQL 仅供参考。
如果您的数据库无法使用此 SQL，建议使用 SpringDataJPA 自带的建表能力，先在开发环境直连测试库自动建表，然后自行导出相关的 SQL 即可。
 */

/*
 Navicat Premium Data Transfer

 Source Server         : Local@3306
 Source Server Type    : MySQL
 Source Server Version : 80300 (8.3.0)
 Source Host           : localhost:3306
 Source Schema         : powerjob5

 Target Server Type    : MySQL
 Target Server Version : 80300 (8.3.0)
 File Encoding         : 65001

 Date: 16/03/2024 22:07:31
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for app_info
-- ----------------------------
DROP TABLE IF EXISTS `app_info`;
CREATE TABLE `app_info` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `app_name` varchar(255) DEFAULT NULL,
                            `creator` bigint DEFAULT NULL,
                            `current_server` varchar(255) DEFAULT NULL,
                            `extra` varchar(255) DEFAULT NULL,
                            `gmt_create` datetime(6) DEFAULT NULL,
                            `gmt_modified` datetime(6) DEFAULT NULL,
                            `modifier` bigint DEFAULT NULL,
                            `namespace_id` bigint DEFAULT NULL,
                            `password` varchar(255) DEFAULT NULL,
                            `tags` varchar(255) DEFAULT NULL,
                            `title` varchar(255) DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uidx01_app_info` (`app_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for container_info
-- ----------------------------
DROP TABLE IF EXISTS `container_info`;
CREATE TABLE `container_info` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `app_id` bigint DEFAULT NULL,
                                  `container_name` varchar(255) DEFAULT NULL,
                                  `gmt_create` datetime(6) DEFAULT NULL,
                                  `gmt_modified` datetime(6) DEFAULT NULL,
                                  `last_deploy_time` datetime(6) DEFAULT NULL,
                                  `source_info` varchar(255) DEFAULT NULL,
                                  `source_type` int DEFAULT NULL,
                                  `status` int DEFAULT NULL,
                                  `version` varchar(255) DEFAULT NULL,
                                  PRIMARY KEY (`id`),
                                  KEY `idx01_container_info` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for instance_info
-- ----------------------------
DROP TABLE IF EXISTS `instance_info`;
CREATE TABLE `instance_info` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `actual_trigger_time` bigint DEFAULT NULL,
                                 `app_id` bigint DEFAULT NULL,
                                 `expected_trigger_time` bigint DEFAULT NULL,
                                 `finished_time` bigint DEFAULT NULL,
                                 `gmt_create` datetime(6) DEFAULT NULL,
                                 `gmt_modified` datetime(6) DEFAULT NULL,
                                 `instance_id` bigint DEFAULT NULL,
                                 `instance_params` longtext,
                                 `job_id` bigint DEFAULT NULL,
                                 `job_params` longtext,
                                 `last_report_time` bigint DEFAULT NULL,
                                 `result` longtext,
                                 `running_times` bigint DEFAULT NULL,
                                 `status` int DEFAULT NULL,
                                 `task_tracker_address` varchar(255) DEFAULT NULL,
                                 `type` int DEFAULT NULL,
                                 `wf_instance_id` bigint DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 KEY `idx01_instance_info` (`job_id`,`status`),
                                 KEY `idx02_instance_info` (`app_id`,`status`),
                                 KEY `idx03_instance_info` (`instance_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for job_info
-- ----------------------------
DROP TABLE IF EXISTS `job_info`;
CREATE TABLE `job_info` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `advanced_runtime_config` varchar(255) DEFAULT NULL,
                            `alarm_config` varchar(255) DEFAULT NULL,
                            `app_id` bigint DEFAULT NULL,
                            `concurrency` int DEFAULT NULL,
                            `designated_workers` varchar(255) DEFAULT NULL,
                            `dispatch_strategy` int DEFAULT NULL,
                            `dispatch_strategy_config` varchar(255) DEFAULT NULL,
                            `execute_type` int DEFAULT NULL,
                            `extra` varchar(255) DEFAULT NULL,
                            `gmt_create` datetime(6) DEFAULT NULL,
                            `gmt_modified` datetime(6) DEFAULT NULL,
                            `instance_retry_num` int DEFAULT NULL,
                            `instance_time_limit` bigint DEFAULT NULL,
                            `job_description` varchar(255) DEFAULT NULL,
                            `job_name` varchar(255) DEFAULT NULL,
                            `job_params` longtext,
                            `lifecycle` varchar(255) DEFAULT NULL,
                            `log_config` varchar(255) DEFAULT NULL,
                            `max_instance_num` int DEFAULT NULL,
                            `max_worker_count` int DEFAULT NULL,
                            `min_cpu_cores` double NOT NULL,
                            `min_disk_space` double NOT NULL,
                            `min_memory_space` double NOT NULL,
                            `next_trigger_time` bigint DEFAULT NULL,
                            `notify_user_ids` varchar(255) DEFAULT NULL,
                            `processor_info` varchar(255) DEFAULT NULL,
                            `processor_type` int DEFAULT NULL,
                            `status` int DEFAULT NULL,
                            `tag` varchar(255) DEFAULT NULL,
                            `task_retry_num` int DEFAULT NULL,
                            `time_expression` varchar(255) DEFAULT NULL,
                            `time_expression_type` int DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            KEY `idx01_job_info` (`app_id`,`status`,`time_expression_type`,`next_trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for namespace
-- ----------------------------
DROP TABLE IF EXISTS `namespace`;
CREATE TABLE `namespace` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `code` varchar(255) DEFAULT NULL,
                             `creator` bigint DEFAULT NULL,
                             `dept` varchar(255) DEFAULT NULL,
                             `extra` varchar(255) DEFAULT NULL,
                             `gmt_create` datetime(6) DEFAULT NULL,
                             `gmt_modified` datetime(6) DEFAULT NULL,
                             `modifier` bigint DEFAULT NULL,
                             `name` varchar(255) DEFAULT NULL,
                             `status` int DEFAULT NULL,
                             `tags` varchar(255) DEFAULT NULL,
                             `token` varchar(255) DEFAULT NULL,
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uidx01_namespace` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for oms_lock
-- ----------------------------
DROP TABLE IF EXISTS `oms_lock`;
CREATE TABLE `oms_lock` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `gmt_create` datetime(6) DEFAULT NULL,
                            `gmt_modified` datetime(6) DEFAULT NULL,
                            `lock_name` varchar(255) DEFAULT NULL,
                            `max_lock_time` bigint DEFAULT NULL,
                            `ownerip` varchar(255) DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uidx01_oms_lock` (`lock_name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for pwjb_user_info
-- ----------------------------
DROP TABLE IF EXISTS `pwjb_user_info`;
CREATE TABLE `pwjb_user_info` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `extra` varchar(255) DEFAULT NULL,
                                  `gmt_create` datetime(6) DEFAULT NULL,
                                  `gmt_modified` datetime(6) DEFAULT NULL,
                                  `password` varchar(255) DEFAULT NULL,
                                  `username` varchar(255) DEFAULT NULL,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uidx01_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for server_info
-- ----------------------------
DROP TABLE IF EXISTS `server_info`;
CREATE TABLE `server_info` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `gmt_create` datetime(6) DEFAULT NULL,
                               `gmt_modified` datetime(6) DEFAULT NULL,
                               `ip` varchar(255) DEFAULT NULL,
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uidx01_server_info` (`ip`),
                               KEY `idx01_server_info` (`gmt_modified`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for sundry
-- ----------------------------
DROP TABLE IF EXISTS `sundry`;
CREATE TABLE `sundry` (
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `content` varchar(255) DEFAULT NULL,
                          `extra` varchar(255) DEFAULT NULL,
                          `gmt_create` datetime(6) DEFAULT NULL,
                          `gmt_modified` datetime(6) DEFAULT NULL,
                          `pkey` varchar(255) DEFAULT NULL,
                          `skey` varchar(255) DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uidx01_sundry` (`pkey`,`skey`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `account_type` varchar(255) DEFAULT NULL,
                             `email` varchar(255) DEFAULT NULL,
                             `extra` varchar(255) DEFAULT NULL,
                             `gmt_create` datetime(6) DEFAULT NULL,
                             `gmt_modified` datetime(6) DEFAULT NULL,
                             `nick` varchar(255) DEFAULT NULL,
                             `origin_username` varchar(255) DEFAULT NULL,
                             `password` varchar(255) DEFAULT NULL,
                             `phone` varchar(255) DEFAULT NULL,
                             `status` int DEFAULT NULL,
                             `token_login_verify_info` varchar(255) DEFAULT NULL,
                             `username` varchar(255) DEFAULT NULL,
                             `web_hook` varchar(255) DEFAULT NULL,
                             PRIMARY KEY (`id`),
                             UNIQUE KEY `uidx01_user_name` (`username`),
                             KEY `uidx02_user_info` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `extra` varchar(255) DEFAULT NULL,
                             `gmt_create` datetime(6) DEFAULT NULL,
                             `gmt_modified` datetime(6) DEFAULT NULL,
                             `role` int DEFAULT NULL,
                             `scope` int DEFAULT NULL,
                             `target` bigint DEFAULT NULL,
                             `user_id` bigint DEFAULT NULL,
                             PRIMARY KEY (`id`),
                             KEY `uidx01_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for workflow_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_info`;
CREATE TABLE `workflow_info` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `app_id` bigint DEFAULT NULL,
                                 `extra` varchar(255) DEFAULT NULL,
                                 `gmt_create` datetime(6) DEFAULT NULL,
                                 `gmt_modified` datetime(6) DEFAULT NULL,
                                 `lifecycle` varchar(255) DEFAULT NULL,
                                 `max_wf_instance_num` int DEFAULT NULL,
                                 `next_trigger_time` bigint DEFAULT NULL,
                                 `notify_user_ids` varchar(255) DEFAULT NULL,
                                 `pedag` longtext,
                                 `status` int DEFAULT NULL,
                                 `time_expression` varchar(255) DEFAULT NULL,
                                 `time_expression_type` int DEFAULT NULL,
                                 `wf_description` varchar(255) DEFAULT NULL,
                                 `wf_name` varchar(255) DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 KEY `idx01_workflow_info` (`app_id`,`status`,`time_expression_type`,`next_trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for workflow_instance_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_instance_info`;
CREATE TABLE `workflow_instance_info` (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `actual_trigger_time` bigint DEFAULT NULL,
                                          `app_id` bigint DEFAULT NULL,
                                          `dag` longtext,
                                          `expected_trigger_time` bigint DEFAULT NULL,
                                          `finished_time` bigint DEFAULT NULL,
                                          `gmt_create` datetime(6) DEFAULT NULL,
                                          `gmt_modified` datetime(6) DEFAULT NULL,
                                          `parent_wf_instance_id` bigint DEFAULT NULL,
                                          `result` longtext,
                                          `status` int DEFAULT NULL,
                                          `wf_context` longtext,
                                          `wf_init_params` longtext,
                                          `wf_instance_id` bigint DEFAULT NULL,
                                          `workflow_id` bigint DEFAULT NULL,
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uidx01_wf_instance` (`wf_instance_id`),
                                          KEY `idx01_wf_instance` (`workflow_id`,`status`,`app_id`,`expected_trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for workflow_node_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_node_info`;
CREATE TABLE `workflow_node_info` (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `app_id` bigint NOT NULL,
                                      `enable` bit(1) NOT NULL,
                                      `extra` longtext,
                                      `gmt_create` datetime(6) NOT NULL,
                                      `gmt_modified` datetime(6) NOT NULL,
                                      `job_id` bigint DEFAULT NULL,
                                      `node_name` varchar(255) DEFAULT NULL,
                                      `node_params` longtext,
                                      `skip_when_failed` bit(1) NOT NULL,
                                      `type` int DEFAULT NULL,
                                      `workflow_id` bigint DEFAULT NULL,
                                      PRIMARY KEY (`id`),
                                      KEY `idx01_workflow_node_info` (`workflow_id`,`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
