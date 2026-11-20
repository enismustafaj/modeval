package com.emu.codemodeval.repos.exception;

/** A repo could not be cloned: git failed, timed out, or was interrupted. */
public class RepoCloneException extends RuntimeException {

    public RepoCloneException(String url, String reason) {
        super("could not clone " + url + ": " + reason);
    }

    public RepoCloneException(String url, String reason, Throwable cause) {
        super("could not clone " + url + ": " + reason, cause);
    }
}
