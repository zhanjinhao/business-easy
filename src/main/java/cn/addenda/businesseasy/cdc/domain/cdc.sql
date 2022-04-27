create table t_change_entity
(
    id           bigint auto_increment
        primary key,
    table_name   varchar(20)  null,
    table_change varchar(500) null,
    create_time  datetime     null
);

create index t_table_change_index
    on t_change_entity (table_name);

create table t_sync_name
(
    id          bigint auto_increment
        primary key,
    sync_name   varchar(20) not null,
    create_time datetime    null
);

create table t_sync_record
(
    id          bigint auto_increment
        primary key,
    table_name  varchar(20) null,
    sync_name   varchar(20) null,
    next        bigint      null,
    create_time datetime    null,
    modify_time datetime    null
);

create index t_sync_record_index
    on t_sync_record (table_name, sync_name, next);

