package com.legara.campusconnect.exception;

public class InvalidRequestException extends CampusConnectException {

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
