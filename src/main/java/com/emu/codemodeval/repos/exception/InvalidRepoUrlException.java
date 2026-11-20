package com.emu.codemodeval.repos.exception;

/** The url is not a plain https GitHub repo url, so it is never passed to git. */
public class InvalidRepoUrlException extends RepoCloneException {

    public InvalidRepoUrlException(String url) {
        super(url, "not a https://github.com/<owner>/<repo> url");
    }
}
