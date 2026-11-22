package com.legara.bookbridge.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.legara.bookbridge.exception.DataAccessException;
import com.legara.bookbridge.model.LibraryBook;
import com.legara.bookbridge.model.LoanRecord;
import com.legara.bookbridge.model.LoanStatus;
import com.legara.bookbridge.model.Member;
import com.legara.bookbridge.model.MembershipType;
import com.legara.bookbridge.model.Reservation;
import com.legara.bookbridge.model.ReservationStatus;

class FakeLibraryStore implements LibraryStore {
    private record MemberRow(String id, String name, MembershipType type) {
    }

    private record BookRow(String isbn, String title) {
    }

    private static class LoanRow {
        String id;
        String memberId;
        String isbn;
        LocalDate loanDate;
        LocalDate dueDate;
        LocalDate returnDate;
        LoanStatus status;
    }

    private static class ReservationRow {
        String id;
        String memberId;
        String isbn;
        LocalDate reservedOn;
        ReservationStatus status;
    }

    private final Map<String, MemberRow> memberRows = new LinkedHashMap<>();
    private final Map<String, BookRow> bookRows = new LinkedHashMap<>();
    private final Map<String, LoanRow> loanRows = new LinkedHashMap<>();
    private final Map<String, ReservationRow> reservationRows = new LinkedHashMap<>();
    private boolean failing;

    void setFailing(boolean failing) {
        this.failing = failing;
    }

    LocalDate storedDueDate(String loanId) {
        return loanRows.get(loanId).dueDate;
    }

    private void checkConnection() throws DataAccessException {
        if (failing) {
            throw new DataAccessException("Simulated outage: Communications link failure",
                    new SQLException("Communications link failure"));
        }
    }

    @Override
    public List<Member> loadMembers() throws DataAccessException {
        checkConnection();
        List<Member> result = new ArrayList<>();
        for (MemberRow row : memberRows.values()) {
            result.add(Member.create(row.type(), row.id(), row.name()));
        }
        return result;
    }

    @Override
    public List<LibraryBook> loadBooks() throws DataAccessException {
        checkConnection();
        List<LibraryBook> result = new ArrayList<>();
        for (BookRow row : bookRows.values()) {
            result.add(new LibraryBook(row.isbn(), row.title()));
        }
        return result;
    }

    @Override
    public List<LoanRecord> loadLoans(Map<String, Member> members, Map<String, LibraryBook> books)
            throws DataAccessException {
        checkConnection();
        List<LoanRecord> result = new ArrayList<>();
        for (LoanRow row : loanRows.values()) {
            result.add(new LoanRecord(row.id, members.get(row.memberId), books.get(row.isbn), row.loanDate,
                    row.dueDate, row.returnDate, row.status));
        }
        return result;
    }

    @Override
    public List<Reservation> loadReservations(Map<String, Member> members, Map<String, LibraryBook> books)
            throws DataAccessException {
        checkConnection();
        List<Reservation> result = new ArrayList<>();
        for (ReservationRow row : reservationRows.values()) {
            result.add(new Reservation(row.id, members.get(row.memberId), books.get(row.isbn), row.reservedOn,
                    row.status));
        }
        return result;
    }

    @Override
    public void insertMember(Member member) throws DataAccessException {
        checkConnection();
        memberRows.put(member.getMemberId(),
                new MemberRow(member.getMemberId(), member.getMemberName(), member.getMembershipType()));
    }

    @Override
    public void insertBook(LibraryBook book) throws DataAccessException {
        checkConnection();
        bookRows.put(book.getIsbn(), new BookRow(book.getIsbn(), book.getTitle()));
    }

    @Override
    public void insertLoan(LoanRecord loan, String fulfilledReservationId) throws DataAccessException {
        checkConnection();
        LoanRow row = new LoanRow();
        row.id = loan.getLoanId();
        row.memberId = loan.getMember().getMemberId();
        row.isbn = loan.getBook().getIsbn();
        row.loanDate = loan.getLoanDate();
        row.dueDate = loan.getDueDate();
        row.status = LoanStatus.ACTIVE;
        loanRows.put(row.id, row);
        if (fulfilledReservationId != null) {
            reservationRows.get(fulfilledReservationId).status = ReservationStatus.FULFILLED;
        }
    }

    @Override
    public void markLoanReturned(String loanId, LocalDate returnDate) throws DataAccessException {
        checkConnection();
        LoanRow row = loanRows.get(loanId);
        row.status = LoanStatus.RETURNED;
        row.returnDate = returnDate;
    }

    @Override
    public void updateDueDate(String loanId, LocalDate newDueDate) throws DataAccessException {
        checkConnection();
        loanRows.get(loanId).dueDate = newDueDate;
    }

    @Override
    public void insertReservation(Reservation reservation) throws DataAccessException {
        checkConnection();
        ReservationRow row = new ReservationRow();
        row.id = reservation.getReservationId();
        row.memberId = reservation.getMember().getMemberId();
        row.isbn = reservation.getBook().getIsbn();
        row.reservedOn = reservation.getReservedOn();
        row.status = reservation.getStatus();
        reservationRows.put(row.id, row);
    }

    @Override
    public void updateReservationStatus(String reservationId, ReservationStatus status) throws DataAccessException {
        checkConnection();
        reservationRows.get(reservationId).status = status;
    }
}
