package com.legara.campusconnect.exception;

public class DataAccessException extends CampusConnectException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
