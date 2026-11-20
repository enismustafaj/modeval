package com.emu.codemodeval.repos.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "repos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Repo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ponytail: plain id, not @ManyToOne; the FK lives in the migration and no join is needed
    @Column(nullable = false, updatable = false)
    private UUID codemodId;

    @Column(nullable = false, updatable = false)
    private String fullName;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private OffsetDateTime clonedAt;

    public static Repo of(UUID codemodId, String fullName) {
        Repo repo = new Repo();
        repo.codemodId = codemodId;
        repo.fullName = fullName;
        return repo;
    }

    public void clonedTo(String url, Path path) {
        this.url = url;
        this.path = path.toString();
        this.clonedAt = OffsetDateTime.now();
    }

    public com.emu.codemodeval.repos.model.Repo toDomain() {
        return new com.emu.codemodeval.repos.model.Repo(codemodId, fullName, url, Path.of(path), clonedAt);
    }
}
