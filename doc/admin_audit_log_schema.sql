-- Required because spring.jpa.hibernate.ddl-auto is validate.
-- Apply once before deploying the admin audit log grouping changes.

alter table system_log
    add column actor_id varchar(255) null,
    add column actor_type varchar(255) null,
    add column action_group varchar(255) null;

create index idx_system_log_actor_type_group_created
    on system_log (actor_type, action_group, created_at);

create index idx_system_log_actor_id_created
    on system_log (actor_id, created_at);
