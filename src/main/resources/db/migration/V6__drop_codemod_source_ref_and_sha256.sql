-- nothing reads them: runs use name@version from the registry, and the hash was client-supplied, never verified.
-- source_ref comes back with git codemods; the hash comes back computed from the downloaded package.
alter table codemods drop column sha256;
alter table codemods drop column source_ref;
