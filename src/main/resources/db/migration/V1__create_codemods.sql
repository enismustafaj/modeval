-- codemods submitted by publishers. rows are immutable: a fix is a new version.
create table codemods (
    id          uuid         not null default gen_random_uuid() primary key,
    name        varchar(255) not null,                -- e.g. "next/15-async-apis"
    version     varchar(64)  not null,
    source      varchar(16)  not null check (source in ('registry', 'git')),
    source_ref  varchar(1024) not null,               -- registry pkg@ver, or git url + commit sha
    sha256      char(64)     not null unique,         -- hash of the fetched content; detects upstream changes
    created_at  timestamp with time zone not null default current_timestamp,
    unique (name, version)
);
