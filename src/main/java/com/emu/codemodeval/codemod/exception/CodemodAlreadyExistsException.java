package com.emu.codemodeval.codemod.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

/** Rendered by Spring as a 409 problem+json response. */
public class CodemodAlreadyExistsException extends ErrorResponseException {

    public CodemodAlreadyExistsException(String name, String version) {
        super(HttpStatus.CONFLICT);
        setDetail("Codemod %s@%s already exists; publish a new version instead".formatted(name, version));
    }
}
