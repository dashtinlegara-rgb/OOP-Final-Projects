package com.legara.campusconnect.exception;

public class CampusConnectException extends Exception {

    public CampusConnectException(String message) {
        super(message);
    }

    public CampusConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
