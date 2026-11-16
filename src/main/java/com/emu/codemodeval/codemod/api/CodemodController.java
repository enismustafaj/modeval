package com.emu.codemodeval.codemod.api;

import com.emu.codemodeval.codemod.api.dto.CodemodResponse;
import com.emu.codemodeval.codemod.api.dto.SubmitCodemodRequest;
import com.emu.codemodeval.codemod.usecase.GetCodemodUseCase;
import com.emu.codemodeval.codemod.usecase.SubmitCodemodUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/codemods")
@RequiredArgsConstructor
public class CodemodController {

    private final SubmitCodemodUseCase submitCodemodUseCase;
    private final GetCodemodUseCase getCodemodUseCase;

    @PostMapping
    public ResponseEntity<CodemodResponse> submit(@Valid @RequestBody SubmitCodemodRequest request,
                                                  UriComponentsBuilder uri) {
        var saved = submitCodemodUseCase.submit(request.toDomain());
        // names can contain "/" (e.g. next/15-async-apis), so they go in query params, not the path
        var location = uri.path("/codemods")
            .queryParam("name", saved.name())
            .queryParam("version", saved.version())
            .encode().build().toUri();
        return ResponseEntity.created(location).body(CodemodResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<CodemodResponse> get(@RequestParam String name, @RequestParam String version) {
        return ResponseEntity.of(getCodemodUseCase.get(name, version).map(CodemodResponse::from));
    }
}
