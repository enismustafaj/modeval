package com.emu.codemodeval.reports.api;

import com.emu.codemodeval.reports.model.Report;
import com.emu.codemodeval.reports.usecase.GetReportUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    // generated from the Report record, so it can't drift from what is stored and served
    private final String schema = JsonSchemaGenerator.generateForType(Report.class);

    private final GetReportUseCase getReport;

    /** 404 until the codemod's job has finished its report step. */
    @GetMapping
    public ResponseEntity<Report> get(@RequestParam String name, @RequestParam String version) {
        return ResponseEntity.of(getReport.forCodemod(name, version));
    }

    @GetMapping(path = "/schema", produces = "application/schema+json")
    public String schema() {
        return schema;
    }
}
