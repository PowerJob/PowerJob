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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='提醒任务原始信息';

select *
from sx_sp_remind_task_info;
