package com.emu.codemodeval.engine.steps.applycodemod.exception;

/** The sandbox itself failed (could not be created, reached, or cleaned up), as opposed to a command in it. */
public class SandboxException extends RuntimeException {

    public SandboxException(String message) {
        super(message);
    }

    public SandboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
