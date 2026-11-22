package com.legara.campusconnect.model;

import java.time.LocalDateTime;

public class EnrollmentRequest extends ServiceRequest {
    public static final int BASE_DAYS = 3;

    public EnrollmentRequest(String ticketId, Student student, int priority, String concern,
            RequestStatus status, LocalDateTime createdAt) {
        super(ticketId, student, priority, concern, status, createdAt);
    }

    @Override
    public ServiceCategory getCategory() {
        return ServiceCategory.ENROLLMENT;
    }

    @Override
    public int estimateDays() {
        return applyPriorityRule(BASE_DAYS);
    }
}
