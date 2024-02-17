-- Upgrade SQL FROM 4.1.x to 4.2.x
-- ----------------------------
-- Table change for app_info
-- ----------------------------
SET FOREIGN_KEY_CHECKS=0;

ALTER TABLE `app_info` ADD COLUMN `creator` bigint NULL DEFAULT NULL;
ALTER TABLE `app_info` ADD COLUMN `extra` varchar(255) NULL DEFAULT NULL;
ALTER TABLE `app_info` ADD COLUMN `modifier` bigint NULL DEFAULT NULL;
ALTER TABLE `app_info` ADD COLUMN `namespace_id` bigint NULL DEFAULT NULL;
ALTER TABLE `app_info` ADD COLUMN `tags` varchar(255)  NULL DEFAULT NULL;
ALTER TABLE `app_info` ADD COLUMN `title` varchar(255)  NULL DEFAULT NULL;

-- ----------------------------
-- Table change for user_info
-- ----------------------------
ALTER TABLE `user_info` ADD COLUMN `account_type` varchar(255)  NULL DEFAULT NULL;
ALTER TABLE `user_info` ADD COLUMN `nick` varchar(255)  NULL DEFAULT NULL;
ALTER TABLE `user_info` ADD COLUMN `origin_username` varchar(255)  NULL DEFAULT NULL;
ALTER TABLE `user_info` ADD COLUMN `token_login_verify_info` varchar(255)  NULL DEFAULT NULL;
ALTER TABLE `user_info` ADD UNIQUE INDEX `uidx01_user_name`(`username` ASC) USING BTREE;

-- ----------------------------
-- new table 'namespace'
-- ----------------------------
CREATE TABLE `namespace`  (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `code` varchar(255)  NULL DEFAULT NULL,
                                          `creator` bigint NULL DEFAULT NULL,
                                          `dept` varchar(255)  NULL DEFAULT NULL,
                                          `extra` varchar(255)  NULL DEFAULT NULL,
                                          `gmt_create` datetime(6) NULL DEFAULT NULL,
                                          `gmt_modified` datetime(6) NULL DEFAULT NULL,
                                          `modifier` bigint NULL DEFAULT NULL,
                                          `name` varchar(255)  NULL DEFAULT NULL,
                                          `status` int NULL DEFAULT NULL,
                                          `tags` varchar(255)  NULL DEFAULT NULL,
                                          `token` varchar(255)  NULL DEFAULT NULL,
                                          PRIMARY KEY (`id`) USING BTREE,
                                          UNIQUE INDEX `uidx01_namespace`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;


-- ----------------------------
-- new table 'pwjb_user_info'
-- ----------------------------
CREATE TABLE `pwjb_user_info`  (
                                               `id` bigint NOT NULL AUTO_INCREMENT,
                                               `extra` varchar(255)  NULL DEFAULT NULL,
                                               `gmt_create` datetime(6) NULL DEFAULT NULL,
                                               `gmt_modified` datetime(6) NULL DEFAULT NULL,
                                               `password` varchar(255)  NULL DEFAULT NULL,
                                               `username` varchar(255)  NULL DEFAULT NULL,
                                               PRIMARY KEY (`id`) USING BTREE,
                                               UNIQUE INDEX `uidx01_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- new table 'sundry'
-- ----------------------------
CREATE TABLE `sundry`  (
                                       `id` bigint NOT NULL AUTO_INCREMENT,
                                       `content` varchar(255)  NULL DEFAULT NULL,
                                       `extra` varchar(255)  NULL DEFAULT NULL,
                                       `gmt_create` datetime(6) NULL DEFAULT NULL,
                                       `gmt_modified` datetime(6) NULL DEFAULT NULL,
                                       `pkey` varchar(255)  NULL DEFAULT NULL,
                                       `skey` varchar(255)  NULL DEFAULT NULL,
                                       PRIMARY KEY (`id`) USING BTREE,
                                       UNIQUE INDEX `uidx01_sundry`(`pkey` ASC, `skey` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;


-- ----------------------------
-- new table 'user_role'
-- ----------------------------
CREATE TABLE `user_role`  (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `extra` varchar(255)  NULL DEFAULT NULL,
                                          `gmt_create` datetime(6) NULL DEFAULT NULL,
                                          `gmt_modified` datetime(6) NULL DEFAULT NULL,
                                          `role` int NULL DEFAULT NULL,
                                          `scope` int NULL DEFAULT NULL,
                                          `target` bigint NULL DEFAULT NULL,
                                          `user_id` bigint NULL DEFAULT NULL,
                                          PRIMARY KEY (`id`) USING BTREE,
                                          INDEX `uidx01_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;