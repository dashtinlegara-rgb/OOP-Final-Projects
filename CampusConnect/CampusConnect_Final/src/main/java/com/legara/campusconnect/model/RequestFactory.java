package com.legara.campusconnect.model;

import java.time.LocalDateTime;

public final class RequestFactory {

    private RequestFactory() {
    }

    public static ServiceRequest create(ServiceCategory category, String ticketId, Student student, int priority,
            String concern, RequestStatus status, LocalDateTime createdAt) {
        return switch (category) {
            case ENROLLMENT -> new EnrollmentRequest(ticketId, student, priority, concern, status, createdAt);
            case DOCUMENT -> new DocumentRequest(ticketId, student, priority, concern, status, createdAt);
            case PAYMENT -> new PaymentConcern(ticketId, student, priority, concern, status, createdAt);
            case ADVISING -> new AdvisingRequest(ticketId, student, priority, concern, status, createdAt);
        };
    }
}
