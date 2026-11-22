package com.legara.bookbridge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.legara.bookbridge.exception.DuplicateEntityException;
import com.legara.bookbridge.exception.EntityNotFoundException;
import com.legara.bookbridge.exception.InvalidOperationException;
import com.legara.bookbridge.exception.ItemUnavailableException;
import com.legara.bookbridge.exception.LoanLimitExceededException;
import com.legara.bookbridge.model.BookStatus;
import com.legara.bookbridge.model.FacultyMember;
import com.legara.bookbridge.model.GuestMember;
import com.legara.bookbridge.model.LibraryBook;
import com.legara.bookbridge.model.LoanRecord;
import com.legara.bookbridge.model.LoanStatus;
import com.legara.bookbridge.model.MembershipType;
import com.legara.bookbridge.model.Reservation;
import com.legara.bookbridge.model.ReservationStatus;
import com.legara.bookbridge.model.StudentMember;

class LibraryServiceTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 22);
    private FakeLibraryStore store;
    private LibraryService service;

    @BeforeEach
    void setUp() throws Exception {
        store = new FakeLibraryStore();
        service = new LibraryService(store);
        service.setToday(START);
        service.addBook(new LibraryBook("333", "Java Basics"));
        service.addBook(new LibraryBook("111", "Algorithms"));
        service.addBook(new LibraryBook("222", "Clean Code"));
        service.addBook(new LibraryBook("444", "Networks"));
        service.registerMember(new StudentMember("S1", "Sam"));
        service.registerMember(new FacultyMember("F1", "Fay"));
        service.registerMember(new GuestMember("G1", "Gina"));
    }

    private void advanceDays(int days) {
        service.setToday(service.today().plusDays(days));
    }

    private String snapshot() {
        return "active=" + service.listActiveLoans().size()
                + " history=" + service.getLoanHistory().size()
                + " pending=" + service.listPendingReservations().size()
                + " out=" + service.filterBooksByStatus(BookStatus.CHECKED_OUT).size()
                + " 333out=" + service.isCheckedOut("333")
                + " members=" + service.listMembers().size()
                + " books=" + service.listBooks().size();
    }

    @Test
    @DisplayName("T01 Duplicate member ID is rejected and the original member is kept")
    void duplicateMember() throws Exception {
        String before = snapshot();
        assertThrows(DuplicateEntityException.class,
                () -> service.registerMember(new GuestMember("S1", "Sara")));
        assertEquals("Sam", service.getMember("S1").getMemberName());
        assertEquals(before, snapshot());
    }

    @Test
    @DisplayName("T02 Issuing a checked-out book throws ItemUnavailable and changes nothing")
    void unavailableBook() throws Exception {
        service.issueLoan("S1", "333", 7);
        String before = snapshot();
        assertThrows(ItemUnavailableException.class, () -> service.issueLoan("F1", "333", 5));
        assertEquals(before, snapshot());
    }

    @Test
    @DisplayName("T03 A 4th active loan for a STUDENT (limit 3) is refused and changes nothing")
    void loanLimit() throws Exception {
        service.issueLoan("S1", "333", 7);
        service.issueLoan("S1", "111", 7);
        service.issueLoan("S1", "222", 7);
        String before = snapshot();
        assertThrows(LoanLimitExceededException.class, () -> service.issueLoan("S1", "444", 7));
        assertEquals(before, snapshot());
        assertFalse(service.isCheckedOut("444"));
    }

    @Test
    @DisplayName("T04 Loan length above the member type's maximum (GUEST 8 > 7) is refused")
    void loanLength() {
        assertThrows(LoanLimitExceededException.class, () -> service.issueLoan("G1", "111", 8));
        assertFalse(service.isCheckedOut("111"));
    }

    @Test
    @DisplayName("T05 Processing an empty reservation queue throws InvalidOperation")
    void emptyQueue() {
        String before = snapshot();
        assertThrows(InvalidOperationException.class, () -> service.processNextReservation());
        assertEquals(before, snapshot());
    }

    @Test
    @DisplayName("T06 Missing searches: unknown ISBN/ID throw or return empty, never null")
    void missingSearch() {
        assertThrows(EntityNotFoundException.class, () -> service.getBook("999"));
        assertThrows(EntityNotFoundException.class, () -> service.getMember("M999"));
        assertTrue(service.searchBooksByTitle("zzz").isEmpty());
        assertTrue(service.loansForMember("M999").isEmpty());
    }

    @Test
    @DisplayName("T07 Sort: overdue first, then due date, then loan ID breaks the tie")
    void sortOrder() throws Exception {
        LoanRecord early = service.issueLoan("F1", "333", 2);
        LoanRecord tieA = service.issueLoan("F1", "111", 10);
        LoanRecord tieB = service.issueLoan("F1", "222", 10);
        advanceDays(3);
        List<LoanRecord> sorted = service.listActiveLoansSorted();
        assertEquals(List.of(early, tieA, tieB), sorted);
        assertTrue(tieA.getLoanId().compareTo(tieB.getLoanId()) < 0);
    }

    @Test
    @DisplayName("T08 Aggregates: counts, active loans per type, and total fines are correct")
    void aggregates() throws Exception {
        service.issueLoan("S1", "333", 7);
        service.issueLoan("G1", "111", 7);
        advanceDays(10);
        CirculationSummary s = service.summary();
        assertEquals(2, s.availableBooks());
        assertEquals(2, s.checkedOutBooks());
        assertEquals(1, s.activeLoansByType().get(MembershipType.STUDENT));
        assertEquals(0, s.activeLoansByType().get(MembershipType.FACULTY));
        assertEquals(1, s.activeLoansByType().get(MembershipType.GUEST));
        assertEquals(3 * 5.0 + 3 * 10.0, s.totalEstimatedFines(), 0.001);
    }

    @Test
    @DisplayName("T09 Return frees the book, keeps the loan in history, and freezes the fine")
    void returnBook() throws Exception {
        LoanRecord loan = service.issueLoan("S1", "333", 7);
        advanceDays(9);
        service.returnBook(loan.getLoanId());
        assertFalse(service.isCheckedOut("333"));
        assertEquals(BookStatus.AVAILABLE, service.getBook("333").getStatus());
        assertEquals(LoanStatus.RETURNED, service.getLoanHistory().get(0).getStatus());
        advanceDays(5);
        assertEquals(10.0, loan.estimatedFine(service.today()), 0.001);
        assertThrows(EntityNotFoundException.class, () -> service.returnBook(loan.getLoanId()));
    }

    @Test
    @DisplayName("T10 Renewing an overdue loan is refused and the fine is kept")
    void overdueRenewal() throws Exception {
        LoanRecord loan = service.issueLoan("S1", "333", 7);
        advanceDays(10);
        assertThrows(InvalidOperationException.class, () -> service.renewLoan(loan.getLoanId(), 7));
        assertEquals(LocalDate.of(2026, 11, 29), loan.getDueDate());
        assertEquals(15.0, loan.estimatedFine(service.today()), 0.001);
    }

    @Test
    @DisplayName("T11 Reservation queue: the first reserver gets the returned book, others are refused")
    void reservationPriority() throws Exception {
        LoanRecord loan = service.issueLoan("S1", "333", 7);
        Reservation r = service.reserveBook("F1", "333");
        service.returnBook(loan.getLoanId());
        assertThrows(ItemUnavailableException.class, () -> service.issueLoan("G1", "333", 3));
        LoanRecord served = service.processNextReservation();
        assertEquals("F1", served.getMember().getMemberId());
        assertEquals(ReservationStatus.FULFILLED, r.getStatus());
        assertTrue(service.listPendingReservations().isEmpty());
    }

    @Test
    @DisplayName("T12 Cancelling a reservation removes it from the queue; cancelling twice is refused")
    void cancelReservation() throws Exception {
        service.issueLoan("S1", "333", 7);
        Reservation r = service.reserveBook("F1", "333");
        service.cancelReservation(r.getReservationId());
        assertEquals(ReservationStatus.CANCELLED, r.getStatus());
        assertTrue(service.listPendingReservations().isEmpty());
        assertThrows(InvalidOperationException.class, () -> service.cancelReservation(r.getReservationId()));
    }

    @Test
    @DisplayName("T13 A duplicate reservation is refused and the queue is unchanged")
    void duplicateReservation() throws Exception {
        service.issueLoan("S1", "333", 7);
        service.reserveBook("F1", "333");
        String before = snapshot();
        assertThrows(DuplicateEntityException.class, () -> service.reserveBook("F1", "333"));
        assertEquals(before, snapshot());
    }

    @Test
    @DisplayName("T14 Read-only views: callers cannot modify the service's collections")
    void defensiveViews() throws Exception {
        service.issueLoan("S1", "333", 7);
        assertThrows(UnsupportedOperationException.class, () -> service.getLoanHistory().clear());
        assertThrows(UnsupportedOperationException.class, () -> service.listMembers().clear());
        assertThrows(UnsupportedOperationException.class, () -> service.listActiveLoansSorted().clear());
        assertEquals(1, service.getLoanHistory().size());
    }
}
