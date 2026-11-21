package com.emu.codemodeval.reports.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID jobId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private com.emu.codemodeval.reports.model.Report content;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public static Report forJob(UUID jobId) {
        Report report = new Report();
        report.jobId = jobId;
        return report;
    }

    public void replaceWith(com.emu.codemodeval.reports.model.Report content) {
        this.content = content;
        this.createdAt = OffsetDateTime.now();
    }
}
