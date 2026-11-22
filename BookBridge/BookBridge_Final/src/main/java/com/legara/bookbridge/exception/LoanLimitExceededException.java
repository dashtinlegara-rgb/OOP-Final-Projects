package com.legara.bookbridge.exception;

public class LoanLimitExceededException extends LibraryException {
    public LoanLimitExceededException(String message) {
        super(message);
    }
}
