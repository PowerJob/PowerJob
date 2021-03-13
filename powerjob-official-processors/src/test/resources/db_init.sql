create table test_table
(
    id           bigint(20) primary key,
    content      varchar(255),
    gmt_create   datetime default now(),
    gmt_modified datetime default now()
);