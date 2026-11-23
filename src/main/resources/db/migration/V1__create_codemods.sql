-- codemods submitted by publishers. rows are immutable: a fix is a new version.
create table codemods (
    id           uuid          not null default gen_random_uuid() primary key,
    name         varchar(255)  not null,                -- e.g. "next/15-async-apis", "@ai-sdk/codemod"
    version      varchar(64)   not null,
    source       varchar(16)   not null check (source in ('registry', 'npm', 'git')),
    args         varchar(1024),                         -- which migration inside the package, e.g. "v7"
    -- what it migrates, as named in the ecosystem's registry; used to find dependent repos
    ecosystem    varchar(16)   not null check (ecosystem in ('npm', 'pypi', 'maven', 'go', 'cargo')),
    package_name varchar(255)  not null,                -- e.g. "next", "org.springframework:spring-core"
    from_range   varchar(64)   not null,                -- versions it migrates from, e.g. "<15"
    to_version   varchar(64),                           -- version it migrates to, e.g. "15"
    created_at   timestamp with time zone not null default current_timestamp,
    unique (name, version)
);
