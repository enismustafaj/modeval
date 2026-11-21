package com.emu.codemodeval.reports.usecase;

import com.emu.codemodeval.reports.model.Report;
import com.emu.codemodeval.reports.persistence.ReportRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaveReportUseCase {

    private final ReportRepository repository;

    /** Stores the job's report. Saving again (a retried job) replaces it. */
    @Transactional
    public void save(UUID jobId, Report report) {
        com.emu.codemodeval.reports.persistence.Report row = repository.findByJobId(jobId)
            .orElseGet(() -> com.emu.codemodeval.reports.persistence.Report.forJob(jobId));
        row.replaceWith(report);
        repository.saveAndFlush(row);
    }
}
