package com.legara.bookbridge.service;

import java.util.Map;

import com.legara.bookbridge.model.MembershipType;

public record CirculationSummary(
        int availableBooks,
        int checkedOutBooks,
        Map<MembershipType, Integer> activeLoansByType,
        double totalEstimatedFines,
        int overdueLoans,
        int pendingReservations,
        int totalLoansEver) {
}
