-- the outcome of a validation job: how the codemod did on each repo. one per job; a retry replaces it.
create table reports (
    id         uuid  not null default gen_random_uuid() primary key,
    job_id     uuid  not null unique references jobs (id),
    content    jsonb not null,
    created_at timestamp with time zone not null default current_timestamp
);
