package org.powerimo.jenkins.pman;

public class PmanJenkinsException extends RuntimeException {
    public PmanJenkinsException(String message) {
        super(message);
    }

    public PmanJenkinsException(String message, Throwable cause) {
        super(message, cause);
    }
}
