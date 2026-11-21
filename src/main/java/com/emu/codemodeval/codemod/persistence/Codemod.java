package com.emu.codemodeval.codemod.persistence;

import com.emu.codemodeval.codemod.model.CodemodSource;
import com.emu.codemodeval.codemod.model.Ecosystem;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Ecosystem targetEcosystem;

    @Column(nullable = false)
    private String targetPackage;

    @Column(nullable = false)
    private String targetVersionRange;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static Codemod fromDomain(com.emu.codemodeval.codemod.model.Codemod c) {
        return Codemod.builder()
            .name(c.name())
            .version(c.version())
            .source(c.source())
            .targetEcosystem(c.targetEcosystem())
            .targetPackage(c.targetPackage())
            .targetVersionRange(c.targetVersionRange())
            .build();
    }

    public com.emu.codemodeval.codemod.model.Codemod toDomain() {
        return new com.emu.codemodeval.codemod.model.Codemod(name, version, source,
            targetEcosystem, targetPackage, targetVersionRange, createdAt);
    }
}
