-- public repos a codemod is validated against, cloned locally by the dependent-repos step.
create table repos (
    id         uuid          not null default gen_random_uuid() primary key,
    codemod_id uuid          not null references codemods (id),
    full_name  varchar(255)  not null,               -- e.g. "vercel/commerce"
    url        varchar(1024) not null,
    path       varchar(1024) not null,               -- local clone
    cloned_at  timestamp with time zone not null default current_timestamp,
    unique (codemod_id, full_name)                   -- a retried job re-clones and replaces the path
);
