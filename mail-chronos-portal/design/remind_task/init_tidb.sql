drop table if exists sx_sp_remind_task_info;
create table sx_sp_remind_task_info
(
    id                bigint primary key,
    col_id            varchar(128) not null comment '集合 ID',
    comp_id           varchar(128) not null comment '组件 ID',
    uid               varchar(128) not null comment '用户ID',
    recurrence_rule   varchar(2048)         default null comment 'iCalendar 重复规则',
    trigger_offset    bigint       not null default 0 comment 'trigger offset',
    time_zone_id      varchar(64)  not null comment '时区',
    param             longtext comment '任务参数',
    extra             longtext comment '附加信息',
    trigger_times     int          not null default 0 comment '触发次数',
    times_limit       int          not null default 0 comment '触发次数限制,<=0 表示不限',
    next_trigger_time bigint       not null comment '下次触发时间',
    start_time        bigint                default null comment '开始时间',
    end_time          bigint                default null comment '结束时间',
    enable            tinyint      not null default 1 comment '是否启用',
    disable_time      datetime              default null comment '被禁用的时间',
    update_time       datetime     not null comment '更新时间',
    create_time       datetime     not null comment '创建时间',
    key idx1_sx_sp_remind_task_info (next_trigger_time, enable),
    key idx2_sx_sp_remind_task_info (uid),
    key idx3_sx_sp_remind_task_info (col_id),
    key idx4_sx_sp_remind_task_info (comp_id)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='提醒任务原始信息';

drop table if exists sx_sp_rt_task_instance;
CREATE TABLE `sx_sp_rt_task_instance`
(
    `id`                    bigint       NOT NULL,
    `task_id`               bigint       NOT NULL COMMENT '任务 ID',
    `custom_id`             varchar(128) NOT NULL COMMENT '业务方定义的 ID，用作查询',
    `custom_key`            varchar(128) NOT NULL COMMENT '业务方定义的 key，用作查询',
    `param`                 longtext COLLATE utf8mb4_general_ci COMMENT '任务参数',
    `extra`                 longtext COLLATE utf8mb4_general_ci COMMENT '附加信息（JSON）',
    `expected_trigger_time` bigint       NOT NULL COMMENT '期望触发时间',
    `actual_trigger_time`   bigint                DEFAULT NULL COMMENT '实际触发时间（记录的是首次执行时间）',
    `finished_time`         bigint                DEFAULT NULL COMMENT '完成时间',
    `running_times`         int          NOT NULL DEFAULT '0' COMMENT '运行次数',
    `max_retry_times`       int          NOT NULL DEFAULT '0' COMMENT '最大重试次数,< 0 代表不限',
    `result`                longtext COLLATE utf8mb4_general_ci COMMENT '执行结果(取决于业务逻辑)',
    `status`                int          NOT NULL DEFAULT '0' COMMENT '状态(执行状态)',
    `enable`                tinyint      NOT NULL DEFAULT '1' COMMENT '是否启用，失败且不需要重试，或者手动停止的这个状态会为置为 0 ',
    `partition_key`         int          not null comment '分区键',
    `update_time`           datetime     NOT NULL COMMENT '更新时间',
    `create_time`           datetime     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`, `partition_key`),
    unique INDEX `udx1_sx_sp_rt_task_instance` (`task_id`,`expected_trigger_time`,`partition_key`),
    KEY `idx1_sx_sp_rt_task_instance` (`expected_trigger_time`, `enable`,`status`),
    KEY `idx2_sx_sp_rt_task_instance` (`custom_id`),
    KEY `idx3_sx_sp_rt_task_instance` (`custom_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='提醒任务实例,rt means remind task'
    PARTITION BY RANGE (partition_key)(PARTITION p0 VALUES LESS THAN (20211028) ENGINE = InnoDB,
        PARTITION p20211028 VALUES LESS THAN (20211029) ENGINE = InnoDB,
        PARTITION p20211029 VALUES LESS THAN (20211030) ENGINE = InnoDB,
        PARTITION p20211030 VALUES LESS THAN (20211031) ENGINE = InnoDB)/** shard_row_id_bits = 2 */;






