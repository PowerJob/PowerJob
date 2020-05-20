/*
 Navicat Premium Data Transfer

 Source Server Type    : MySQL
 Source Server Version : 50724
 Source Schema         : oms-product

 Target Server Type    : MySQL
 Target Server Version : 50724
 File Encoding         : 65001

 Date: 20/05/2020 16:28:20
*/
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for app_info
-- ----------------------------
DROP TABLE IF EXISTS `app_info`;
CREATE TABLE `app_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_name` varchar(255) DEFAULT NULL,
  `current_server` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `gmt_create` datetime(6) DEFAULT NULL,
  `gmt_modified` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `appNameUK` (`app_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for container_info
-- ----------------------------
DROP TABLE IF EXISTS `container_info`;
CREATE TABLE `container_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) DEFAULT NULL,
  `container_name` varchar(255) DEFAULT NULL,
  `gmt_create` datetime(6) DEFAULT NULL,
  `gmt_modified` datetime(6) DEFAULT NULL,
  `last_deploy_time` datetime(6) DEFAULT NULL,
  `source_info` varchar(255) DEFAULT NULL,
  `source_type` int(11) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `version` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `containerNameUK` (`container_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for instance_info
-- ----------------------------
DROP TABLE IF EXISTS `instance_info`;
CREATE TABLE `instance_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `actual_trigger_time` bigint(20) DEFAULT NULL,
  `app_id` bigint(20) DEFAULT NULL,
  `expected_trigger_time` bigint(20) DEFAULT NULL,
  `finished_time` bigint(20) DEFAULT NULL,
  `gmt_create` datetime(6) DEFAULT NULL,
  `gmt_modified` datetime(6) DEFAULT NULL,
  `instance_id` bigint(20) DEFAULT NULL,
  `instance_params` varchar(255) DEFAULT NULL,
  `job_id` bigint(20) DEFAULT NULL,
  `result` varchar(255) DEFAULT NULL,
  `running_times` bigint(20) DEFAULT NULL,
  `status` int(11) NOT NULL,
  `task_tracker_address` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDX5b1nhpe5je7gc5s1ur200njr7` (`job_id`),
  KEY `IDXjnji5lrr195kswk6f7mfhinrs` (`app_id`),
  KEY `IDXa98hq3yu0l863wuotdjl7noum` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for job_info
-- ----------------------------
DROP TABLE IF EXISTS `job_info`;
CREATE TABLE `job_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) DEFAULT NULL,
  `concurrency` int(11) DEFAULT NULL,
  `designated_workers` varchar(255) DEFAULT NULL,
  `execute_type` int(11) DEFAULT NULL,
  `gmt_create` datetime(6) DEFAULT NULL,
  `gmt_modified` datetime(6) DEFAULT NULL,
  `instance_retry_num` int(11) DEFAULT NULL,
  `instance_time_limit` bigint(20) DEFAULT NULL,
  `job_description` varchar(255) DEFAULT NULL,
  `job_name` varchar(255) DEFAULT NULL,
  `job_params` varchar(255) DEFAULT NULL,
  `max_instance_num` int(11) DEFAULT NULL,
  `max_worker_count` int(11) DEFAULT NULL,
  `min_cpu_cores` double NOT NULL,
  `min_disk_space` double NOT NULL,
  `min_memory_space` double NOT NULL,
  `next_trigger_time` bigint(20) DEFAULT NULL,
  `notify_user_ids` varchar(255) DEFAULT NULL,
  `processor_info` varchar(255) DEFAULT NULL,
  `processor_type` int(11) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `task_retry_num` int(11) DEFAULT NULL,
  `time_expression` varchar(255) DEFAULT NULL,
  `time_expression_type` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `IDXk2xprmn3lldmlcb52i36udll1` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for oms_lock
-- ----------------------------
DROP TABLE IF EXISTS `oms_lock`;
CREATE TABLE `oms_lock` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime(6) DEFAULT NULL,
  `gmt_modified` datetime(6) DEFAULT NULL,
  `lock_name` varchar(255) DEFAULT NULL,
  `max_lock_time` bigint(20) DEFAULT NULL,
  `ownerip` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `lockNameUK` (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for server_info
-- ----------------------------
DROP TABLE IF EXISTS `server_info`;
CREATE TABLE `server_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime(6) DEFAULT NULL,
  `gmt_modified` datetime(6) DEFAULT NULL,
  `ip` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtk8ytgpl7mpukhnvhbl82kgvy` (`ip`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) DEFAULT NULL,
  `gmt_create` datetime(6) DEFAULT NULL,
  `gmt_modified` datetime(6) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
