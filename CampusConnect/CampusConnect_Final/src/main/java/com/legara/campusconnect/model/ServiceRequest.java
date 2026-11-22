package com.legara.campusconnect.model;

import java.time.LocalDateTime;
import java.util.Objects;

public abstract class ServiceRequest implements Prioritizable {
    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 5;
    public static final int URGENT_PRIORITY = 5;
    public static final int MIN_ESTIMATE_DAYS = 1;
    public static final int MAX_TICKET_LENGTH = 10;
    public static final int MAX_CONCERN_LENGTH = 255;

    private final String ticketId;
    private final Student student;
    private final int priority;
    private final String concern;
    private final LocalDateTime createdAt;
    private RequestStatus status;

    protected ServiceRequest(String ticketId, Student student, int priority, String concern,
            RequestStatus status, LocalDateTime createdAt) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket ID cannot be blank.");
        }
        Objects.requireNonNull(student, "Student is required.");
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException("Priority must be from " + MIN_PRIORITY + " to " + MAX_PRIORITY + ".");
        }
        if (concern == null || concern.trim().isEmpty()) {
            throw new IllegalArgumentException("Concern cannot be blank.");
        }
        if (ticketId.trim().length() > MAX_TICKET_LENGTH) {
            throw new IllegalArgumentException("Ticket ID must be at most " + MAX_TICKET_LENGTH + " characters.");
        }
        if (concern.trim().length() > MAX_CONCERN_LENGTH) {
            throw new IllegalArgumentException("Concern must be at most " + MAX_CONCERN_LENGTH + " characters.");
        }
        this.ticketId = ticketId.trim();
        this.student = student;
        this.priority = priority;
        this.concern = concern.trim();
        this.status = Objects.requireNonNull(status, "Status is required.");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time is required.");
    }

    public abstract ServiceCategory getCategory();

    public abstract int estimateDays();

    protected int applyPriorityRule(int baseDays) {
        int days = isUrgent() ? baseDays - 1 : baseDays;
        return Math.max(MIN_ESTIMATE_DAYS, days);
    }

    public void changeStatus(RequestStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Cannot change " + ticketId + " from " + status + " to " + next + ".");
        }
        status = next;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean isUrgent() {
        return priority == URGENT_PRIORITY;
    }

    public boolean isOpen() {
        return status.isOpen();
    }

    public String getTicketId() {
        return ticketId;
    }

    public Student getStudent() {
        return student;
    }

    public String getConcern() {
        return concern;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return ticketId + " " + getCategory() + " " + status + " for " + student + ", priority " + priority
                + ", estimate " + estimateDays() + " day(s)";
    }
}
