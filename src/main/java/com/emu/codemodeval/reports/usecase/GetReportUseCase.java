package com.emu.codemodeval.reports.usecase;

import com.emu.codemodeval.reports.model.Report;
import com.emu.codemodeval.reports.persistence.ReportRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetReportUseCase {

    private final ReportRepository repository;

    @Transactional(readOnly = true)
    public Optional<Report> forJob(UUID jobId) {
        return repository.findByJobId(jobId).map(com.emu.codemodeval.reports.persistence.Report::getContent);
    }

    @Transactional(readOnly = true)
    public Optional<Report> forCodemod(String name, String version) {
        return repository.findLatestForCodemod(name, version).map(com.emu.codemodeval.reports.persistence.Report::getContent);
    }
}
