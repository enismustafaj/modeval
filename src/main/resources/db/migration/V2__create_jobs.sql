-- one validation job per submitted codemod. the worker claims queued jobs with "for update skip locked".
create table jobs (
    id           uuid         not null default gen_random_uuid() primary key,
    codemod_id   uuid         not null references codemods (id),
    status       varchar(16)  not null check (status in ('queued', 'running', 'succeeded', 'failed')),
    error        varchar(2000),
    created_at   timestamp with time zone not null default current_timestamp,
    started_at   timestamp with time zone,
    finished_at  timestamp with time zone
);

create index jobs_status_created_at_idx on jobs (status, created_at);
create index jobs_codemod_id_idx on jobs (codemod_id);
