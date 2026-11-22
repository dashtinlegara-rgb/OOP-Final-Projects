package com.legara.bookbridge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.legara.bookbridge.app.ConcurrencyDemo;
import com.legara.bookbridge.exception.DataAccessException;
import com.legara.bookbridge.exception.LibraryException;
import com.legara.bookbridge.model.BookStatus;
import com.legara.bookbridge.model.FacultyMember;
import com.legara.bookbridge.model.GuestMember;
import com.legara.bookbridge.model.LibraryBook;
import com.legara.bookbridge.model.LoanRecord;
import com.legara.bookbridge.model.LoanStatus;
import com.legara.bookbridge.model.ReservationStatus;
import com.legara.bookbridge.model.StudentMember;

class PersistenceAndConcurrencyTest {
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
        service.registerMember(new StudentMember("S1", "Sam"));
        service.registerMember(new FacultyMember("F1", "Fay"));
        service.registerMember(new GuestMember("G1", "Gina"));
    }

    private String state() {
        return "active=" + service.listActiveLoans().size()
                + " history=" + service.getLoanHistory().size()
                + " pending=" + service.listPendingReservations().size()
                + " 333out=" + service.isCheckedOut("333")
                + " members=" + service.listMembers().size()
                + " books=" + service.listBooks().size();
    }

    @Test
    @DisplayName("T15 Database outage: the operation fails with DataAccessException and memory is unchanged")
    void outageLeavesStateUnchanged() throws Exception {
        String before = state();
        store.setFailing(true);
        DataAccessException e = assertThrows(DataAccessException.class, () -> service.issueLoan("S1", "333", 7));
        assertNotNull(e.getCause());
        assertThrows(DataAccessException.class, () -> service.addBook(new LibraryBook("999", "Outage Book")));
        assertEquals(before, state());
        store.setFailing(false);
        service.issueLoan("S1", "333", 7);
        assertTrue(service.isCheckedOut("333"));
    }

    @Test
    @DisplayName("T16 Restart: a new service loading from the store sees the same members, loans and reservations")
    void dataSurvivesRestart() throws Exception {
        LoanRecord samLoan = service.issueLoan("S1", "333", 7);
        service.issueLoan("G1", "111", 5);
        service.reserveBook("F1", "333");
        service.returnBook(samLoan.getLoanId());

        LibraryService restarted = new LibraryService(store);
        restarted.setToday(START);
        restarted.loadFromStore();

        assertEquals(3, restarted.listMembers().size());
        assertEquals(2, restarted.listBooks().size());
        assertEquals(1, restarted.listActiveLoans().size());
        assertEquals(2, restarted.getLoanHistory().size());
        assertEquals(LoanStatus.RETURNED, restarted.getLoanHistory().get(0).getStatus());
        assertEquals(BookStatus.CHECKED_OUT, restarted.getBook("111").getStatus());
        assertEquals(BookStatus.AVAILABLE, restarted.getBook("333").getStatus());
        assertEquals(1, restarted.listPendingReservations().size());
        assertEquals(ReservationStatus.PENDING, restarted.listPendingReservations().get(0).getStatus());
    }

    @Test
    @DisplayName("T17 Quote/injection-like text is stored and read back as plain data")
    void injectionLikeText() throws Exception {
        String name = "Robert'); DROP TABLE member;--";
        String title = "O'Reilly \"Java\" 100% ; --";
        service.registerMember(new StudentMember("S9", name));
        service.addBook(new LibraryBook("777", title));

        LibraryService restarted = new LibraryService(store);
        restarted.loadFromStore();
        assertEquals(name, restarted.getMember("S9").getMemberName());
        assertEquals(title, restarted.getBook("777").getTitle());
        assertEquals(4, restarted.listMembers().size());
    }

    @Test
    @DisplayName("T18 Two threads processing the same reservation at once issue exactly one loan")
    void concurrentProcessNext() throws Exception {
        LoanRecord loan = service.issueLoan("S1", "333", 7);
        service.reserveBook("F1", "333");
        service.returnBook(loan.getLoanId());

        List<String> outcomes = Collections.synchronizedList(new ArrayList<>());
        Runnable worker = () -> {
            try {
                service.processNextReservation();
                outcomes.add("issued");
            } catch (LibraryException e) {
                outcomes.add("refused");
            }
        };
        Thread first = new Thread(worker);
        Thread second = new Thread(worker);
        first.start();
        second.start();
        first.join();
        second.join();

        assertEquals(1, Collections.frequency(outcomes, "issued"));
        assertEquals(1, Collections.frequency(outcomes, "refused"));
        assertEquals(1, service.listActiveLoans().size());
        assertTrue(service.listPendingReservations().isEmpty());
    }

    @Test
    @DisplayName("T19 Shared summary counter: the synchronized version never loses updates")
    void synchronizedCounterIsExact() throws Exception {
        for (int run = 0; run < 3; run++) {
            assertEquals(ConcurrencyDemo.expectedTotal(), ConcurrencyDemo.countSafe());
        }
    }

    @Test
    @DisplayName("T20 A renewal is saved to the store before memory changes")
    void renewalIsPersisted() throws Exception {
        LoanRecord loan = service.issueLoan("F1", "333", 5);
        service.renewLoan(loan.getLoanId(), 3);
        assertEquals(START.plusDays(8), loan.getDueDate());
        assertEquals(START.plusDays(8), store.storedDueDate(loan.getLoanId()));

        store.setFailing(true);
        assertThrows(DataAccessException.class, () -> service.renewLoan(loan.getLoanId(), 1));
        assertEquals(START.plusDays(8), loan.getDueDate());
        assertFalse(loan.getDueDate().isAfter(START.plusDays(8)));
    }
}
