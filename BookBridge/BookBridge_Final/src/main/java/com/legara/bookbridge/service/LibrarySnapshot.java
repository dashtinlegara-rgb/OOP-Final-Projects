package com.legara.bookbridge.service;

import java.time.LocalDate;
import java.util.List;

import com.legara.bookbridge.model.LibraryBook;
import com.legara.bookbridge.model.LoanRecord;
import com.legara.bookbridge.model.Member;
import com.legara.bookbridge.model.Reservation;

public record LibrarySnapshot(
        LocalDate today,
        List<LibraryBook> books,
        List<Member> members,
        List<LoanRecord> activeLoans,
        List<Reservation> pendingReservations,
        CirculationSummary summary) {
}
