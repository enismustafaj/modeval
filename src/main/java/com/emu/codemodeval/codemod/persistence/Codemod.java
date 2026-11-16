package com.emu.codemodeval.codemod.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import com.emu.codemodeval.codemod.model.CodemodSource;

@Entity
@Table(name = "codemods")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Codemod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CodemodSource source;

    @Column(nullable = false)
    private String sourceRef;

    @Column(nullable = false, unique = true, columnDefinition = "char(64)")
    private String sha256;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static Codemod fromDomain(com.emu.codemodeval.codemod.model.Codemod c) {
        return Codemod.builder()
            .name(c.name())
            .version(c.version())
            .source(c.source())
            .sourceRef(c.sourceRef())
            .sha256(c.sha256())
            .build();
    }

    public com.emu.codemodeval.codemod.model.Codemod toDomain() {
        return new com.emu.codemodeval.codemod.model.Codemod(name, version, source, sourceRef, sha256, createdAt);
    }
}
