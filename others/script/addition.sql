create table sx_user_role
(
    id           bigint auto_increment
        primary key,
    gmt_create   datetime     null,
    gmt_modified datetime     null,
    role         varchar(255) null,
    user_id      bigint       not null,
    index idx01_sx_user_role(user_id)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='用户角色表';