USE powerjob-daily;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `app_info` (
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '应用ID',
    `app_name`       varchar(128) not NULL COMMENT '应用名称',
    `current_server` varchar(255) default null COMMENT 'Server地址,用于负责调度应用的ActorSystem地址',
    `gmt_create`     datetime     not null COMMENT '创建时间',
    `gmt_modified`   datetime     not null COMMENT '更新时间',
    `password`       varchar(255) not null COMMENT '应用密码',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uidx01_app_info` (`app_name`)
) ENGINE = InnoDB AUTO_INCREMENT = 1
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_general_ci COMMENT ='应用表';

insert into app_info (app_name, gmt_create, gmt_modified, password) select 'powerjob-worker-samples', current_timestamp(), current_timestamp(), 'powerjob123' from dual where not exists ( select * from app_info where app_name = 'powerjob-worker-samples');

SET FOREIGN_KEY_CHECKS = 1;