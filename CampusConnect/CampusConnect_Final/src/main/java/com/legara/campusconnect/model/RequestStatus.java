package com.legara.campusconnect.model;

public enum RequestStatus {
    PENDING, IN_PROGRESS, RESOLVED, CANCELLED;

    public boolean canTransitionTo(RequestStatus next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == RESOLVED || next == CANCELLED;
            case RESOLVED, CANCELLED -> false;
        };
    }

    public boolean isOpen() {
        return this == PENDING || this == IN_PROGRESS;
    }
}
