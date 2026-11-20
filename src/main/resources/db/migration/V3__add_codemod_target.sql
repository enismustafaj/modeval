-- the package a codemod migrates, as named in its ecosystem's registry. used to find dependent repos.
alter table codemods add column target_ecosystem     varchar(16)  not null check (target_ecosystem in ('npm', 'pypi', 'maven', 'go', 'cargo'));
alter table codemods add column target_package       varchar(255) not null;  -- e.g. "next", "org.springframework:spring-core"
alter table codemods add column target_version_range varchar(64)  not null;  -- versions the codemod migrates from, e.g. "<15"
