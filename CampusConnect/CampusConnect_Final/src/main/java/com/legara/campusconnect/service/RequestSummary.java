package com.legara.campusconnect.service;

import java.util.Map;

import com.legara.campusconnect.model.RequestStatus;
import com.legara.campusconnect.model.ServiceCategory;

public record RequestSummary(
        Map<ServiceCategory, Integer> countByCategory,
        Map<RequestStatus, Integer> countByStatus,
        double averageEstimatedDays,
        int totalRequests,
        int pendingInQueue) {
}
