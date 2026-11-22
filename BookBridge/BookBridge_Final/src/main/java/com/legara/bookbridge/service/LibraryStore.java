package com.legara.bookbridge.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.legara.bookbridge.exception.DataAccessException;
import com.legara.bookbridge.model.LibraryBook;
import com.legara.bookbridge.model.LoanRecord;
import com.legara.bookbridge.model.Member;
import com.legara.bookbridge.model.Reservation;
import com.legara.bookbridge.model.ReservationStatus;

public interface LibraryStore {
    List<Member> loadMembers() throws DataAccessException;

    List<LibraryBook> loadBooks() throws DataAccessException;

    List<LoanRecord> loadLoans(Map<String, Member> members, Map<String, LibraryBook> books) throws DataAccessException;

    List<Reservation> loadReservations(Map<String, Member> members, Map<String, LibraryBook> books)
            throws DataAccessException;

    void insertMember(Member member) throws DataAccessException;

    void insertBook(LibraryBook book) throws DataAccessException;

    void insertLoan(LoanRecord loan, String fulfilledReservationId) throws DataAccessException;

    void markLoanReturned(String loanId, LocalDate returnDate) throws DataAccessException;

    void updateDueDate(String loanId, LocalDate newDueDate) throws DataAccessException;

    void insertReservation(Reservation reservation) throws DataAccessException;

    void updateReservationStatus(String reservationId, ReservationStatus status) throws DataAccessException;
}
