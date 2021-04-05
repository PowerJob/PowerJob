/*
 Navicat Premium Data Transfer

 Source Server         : Local@3306
 Source Server Type    : MySQL
 Source Server Version : 80021
 Source Host           : localhost:3306
 Source Schema         : powerjob-daily

 Target Server Type    : MySQL
 Target Server Version : 80021
 File Encoding         : 65001

 Date: 05/04/2021 13:15:19
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
                            `current_server` varchar(255) DEFAULT NULL,
                            `gmt_create` datetime(6) DEFAULT NULL,
                            `gmt_modified` datetime(6) DEFAULT NULL,
                            `password` varchar(255) DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `appNameUK` (`app_name`)
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
                                  KEY `IDX8hixyaktlnwil2w9up6b0p898` (`app_id`)
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
                                 `result` text,
                                 `running_times` bigint DEFAULT NULL,
                                 `status` int DEFAULT NULL,
                                 `task_tracker_address` varchar(255) DEFAULT NULL,
                                 `type` int DEFAULT NULL,
                                 `wf_instance_id` bigint DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 KEY `IDX5b1nhpe5je7gc5s1ur200njr7` (`job_id`),
                                 KEY `IDXjnji5lrr195kswk6f7mfhinrs` (`app_id`),
                                 KEY `IDXa98hq3yu0l863wuotdjl7noum` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for job_info
-- ----------------------------
DROP TABLE IF EXISTS `job_info`;
CREATE TABLE `job_info` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `app_id` bigint DEFAULT NULL,
                            `concurrency` int DEFAULT NULL,
                            `designated_workers` varchar(255) DEFAULT NULL,
                            `dispatch_strategy` int DEFAULT NULL,
                            `execute_type` int DEFAULT NULL,
                            `extra` varchar(255) DEFAULT NULL,
                            `gmt_create` datetime(6) DEFAULT NULL,
                            `gmt_modified` datetime(6) DEFAULT NULL,
                            `instance_retry_num` int DEFAULT NULL,
                            `instance_time_limit` bigint DEFAULT NULL,
                            `job_description` varchar(255) DEFAULT NULL,
                            `job_name` varchar(255) DEFAULT NULL,
                            `job_params` text,
                            `lifecycle` varchar(255) DEFAULT NULL,
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
                            `task_retry_num` int DEFAULT NULL,
                            `time_expression` varchar(255) DEFAULT NULL,
                            `time_expression_type` int DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            KEY `IDXk2xprmn3lldmlcb52i36udll1` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
                            UNIQUE KEY `lockNameUK` (`lock_name`)
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
                               UNIQUE KEY `UKtk8ytgpl7mpukhnvhbl82kgvy` (`ip`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
                             `id` bigint NOT NULL AUTO_INCREMENT,
                             `email` varchar(255) DEFAULT NULL,
                             `extra` varchar(255) DEFAULT NULL,
                             `gmt_create` datetime(6) DEFAULT NULL,
                             `gmt_modified` datetime(6) DEFAULT NULL,
                             `password` varchar(255) DEFAULT NULL,
                             `phone` varchar(255) DEFAULT NULL,
                             `username` varchar(255) DEFAULT NULL,
                             `web_hook` varchar(255) DEFAULT NULL,
                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
                                 `pedag` text,
                                 `status` int DEFAULT NULL,
                                 `time_expression` varchar(255) DEFAULT NULL,
                                 `time_expression_type` int DEFAULT NULL,
                                 `wf_description` varchar(255) DEFAULT NULL,
                                 `wf_name` varchar(255) DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 KEY `IDX7uo5w0e3beeho3fnx9t7eiol3` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for workflow_instance_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_instance_info`;
CREATE TABLE `workflow_instance_info` (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `actual_trigger_time` bigint DEFAULT NULL,
                                          `app_id` bigint DEFAULT NULL,
                                          `dag` text,
                                          `expected_trigger_time` bigint DEFAULT NULL,
                                          `finished_time` bigint DEFAULT NULL,
                                          `gmt_create` datetime(6) DEFAULT NULL,
                                          `gmt_modified` datetime(6) DEFAULT NULL,
                                          `result` text,
                                          `status` int DEFAULT NULL,
                                          `wf_context` text,
                                          `wf_init_params` text,
                                          `wf_instance_id` bigint DEFAULT NULL,
                                          `workflow_id` bigint DEFAULT NULL,
                                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for workflow_node_info
-- ----------------------------
DROP TABLE IF EXISTS `workflow_node_info`;
CREATE TABLE `workflow_node_info` (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `app_id` bigint NOT NULL,
                                      `enable` bit(1) NOT NULL,
                                      `extra` text,
                                      `gmt_create` datetime(6) NOT NULL,
                                      `gmt_modified` datetime(6) NOT NULL,
                                      `job_id` bigint DEFAULT NULL,
                                      `node_name` varchar(255) DEFAULT NULL,
                                      `node_params` text,
                                      `skip_when_failed` bit(1) NOT NULL,
                                      `type` int DEFAULT NULL,
                                      `workflow_id` bigint DEFAULT NULL,
                                      PRIMARY KEY (`id`),
                                      KEY `IDX36t7rhj4mkg2a5pb4ttorscta` (`app_id`),
                                      KEY `IDXacr0i6my8jr002ou8i1gmygju` (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
