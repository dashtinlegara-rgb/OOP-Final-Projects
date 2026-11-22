package com.legara.campusconnect.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.legara.campusconnect.exception.DataAccessException;
import com.legara.campusconnect.exception.DuplicateTicketException;
import com.legara.campusconnect.exception.InvalidRequestException;
import com.legara.campusconnect.exception.InvalidStatusTransitionException;
import com.legara.campusconnect.exception.RequestNotFoundException;
import com.legara.campusconnect.model.RequestStatus;
import com.legara.campusconnect.model.ServiceCategory;
import com.legara.campusconnect.model.ServiceRequest;

class RequestServiceTest {

    private FakeRequestStore store;
    private RequestService service;

    @BeforeEach
    void setUp() throws Exception {
        store = new FakeRequestStore();
        service = new RequestService(store);
        service.reload();
    }

    private String snapshot() {
        return "size=" + service.size() + " queue=" + service.pendingCount() + " rows=" + store.rowCount()
                + " sorted=" + service.listSorted();
    }

    @Test
    @DisplayName("C01 Estimate rule: base days per category, priority 5 subtracts one, never below one")
    void estimateRule() throws Exception {
        assertEquals(3, service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x").estimateDays());
        assertEquals(1, service.submit("S2", "Ben", ServiceCategory.DOCUMENT, 5, "x").estimateDays());
        assertEquals(1, service.submit("S3", "Cy", ServiceCategory.PAYMENT, 5, "x").estimateDays());
        assertEquals(3, service.submit("S4", "Di", ServiceCategory.ADVISING, 5, "x").estimateDays());
    }

    @Test
    @DisplayName("C02 Duplicate ticket ID is rejected before any database write")
    void duplicateTicket() throws Exception {
        service.add("T0100", "S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x");
        String before = snapshot();
        assertThrows(DuplicateTicketException.class,
                () -> service.add("T0100", "S2", "Ben", ServiceCategory.DOCUMENT, 2, "y"));
        assertEquals(before, snapshot());
    }

    @Test
    @DisplayName("C03 Invalid values (priority 0/6, blank name, too-long concern, null category) are rejected")
    void invalidValues() {
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 6, "x"));
        assertTrue(e.getCause() instanceof IllegalArgumentException);
        assertThrows(InvalidRequestException.class, () -> service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 0, "x"));
        assertThrows(InvalidRequestException.class, () -> service.submit("S1", "  ", ServiceCategory.ENROLLMENT, 3, "x"));
        assertThrows(InvalidRequestException.class, () -> service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x".repeat(256)));
        assertThrows(InvalidRequestException.class, () -> service.submit("S1", "Ana", null, 3, "x"));
        assertEquals(0, service.size());
        assertEquals(0, store.rowCount());
    }

    @Test
    @DisplayName("C04 Set rule: one open request per student per category; allowed again after it closes")
    void openCategoryRule() throws Exception {
        ServiceRequest first = service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x");
        assertThrows(InvalidRequestException.class, () -> service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 2, "y"));
        service.updateStatus(first.getTicketId(), RequestStatus.CANCELLED);
        service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 2, "z");
        assertEquals(2, service.size());
    }

    @Test
    @DisplayName("C05 Invalid status transitions are refused; memory and database keep the old status")
    void invalidTransition() throws Exception {
        ServiceRequest r = service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x");
        assertThrows(InvalidStatusTransitionException.class, () -> service.updateStatus(r.getTicketId(), RequestStatus.RESOLVED));
        assertEquals(RequestStatus.PENDING, r.getStatus());
        assertEquals(RequestStatus.PENDING, store.storedStatus(r.getTicketId()));
    }

    @Test
    @DisplayName("C06 Processing an empty queue throws RequestNotFoundException")
    void emptyQueue() {
        assertThrows(RequestNotFoundException.class, () -> service.processNext());
    }

    @Test
    @DisplayName("C07 Process next takes the highest priority first and persists IN_PROGRESS")
    void processOrder() throws Exception {
        service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 2, "x");
        ServiceRequest urgent = service.submit("S2", "Ben", ServiceCategory.DOCUMENT, 5, "y");
        assertSame(urgent, service.processNext());
        assertEquals(RequestStatus.IN_PROGRESS, store.storedStatus(urgent.getTicketId()));
        assertEquals("T0001", service.processNext().getTicketId());
    }

    @Test
    @DisplayName("C08 No-result queries: unknown ticket/student give empty lists, not errors or null")
    void missingSearch() throws Exception {
        assertTrue(service.lookup("T9999").isEmpty());
        assertTrue(service.lookup("   ").isEmpty());
        assertTrue(service.searchByStudent("nobody").isEmpty());
        assertThrows(RequestNotFoundException.class, () -> service.remove("T9999"));
    }

    @Test
    @DisplayName("C09 Sort: priority desc, then estimated days asc, then ticket ID on a tie")
    void sortTie() throws Exception {
        ServiceRequest a = service.add("T0003", "S1", "Ana", ServiceCategory.DOCUMENT, 3, "x");
        ServiceRequest b = service.add("T0001", "S2", "Ben", ServiceCategory.DOCUMENT, 3, "x");
        ServiceRequest c = service.add("T0002", "S3", "Cy", ServiceCategory.ADVISING, 3, "x");
        ServiceRequest d = service.add("T0004", "S4", "Di", ServiceCategory.PAYMENT, 4, "x");
        assertEquals(List.of(d, b, a, c), service.listSorted());
    }

    @Test
    @DisplayName("C10 Aggregates: count by category, count by status, average estimated days")
    void aggregates() throws Exception {
        service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x");
        service.submit("S2", "Ben", ServiceCategory.DOCUMENT, 5, "x");
        service.submit("S3", "Cy", ServiceCategory.PAYMENT, 5, "x");
        service.submit("S4", "Di", ServiceCategory.ADVISING, 2, "x");
        service.submit("S5", "Ed", ServiceCategory.ADVISING, 5, "x");
        service.processNext();
        RequestSummary s = service.summary();
        assertEquals(2, s.countByCategory().get(ServiceCategory.ADVISING));
        assertEquals(4, s.countByStatus().get(RequestStatus.PENDING));
        assertEquals(1, s.countByStatus().get(RequestStatus.IN_PROGRESS));
        assertEquals(2.4, s.averageEstimatedDays(), 0.0001);
    }

    @Test
    @DisplayName("C11 Database outage on Add: DataAccessException with SQLException cause; nothing changes")
    void outageOnAdd() {
        store.setDown(true);
        String before = snapshot();
        DataAccessException e = assertThrows(DataAccessException.class,
                () -> service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x"));
        assertTrue(e.getCause() instanceof SQLException);
        store.setDown(false);
        assertEquals(before, snapshot());
        assertFalse(service.hasOpenRequest("S1", ServiceCategory.ENROLLMENT));
    }

    @Test
    @DisplayName("C12 Database outage on Update/Delete/Process Next leaves memory unchanged")
    void outageOnWrites() throws Exception {
        ServiceRequest r = service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x");
        store.setDown(true);
        assertThrows(DataAccessException.class, () -> service.updateStatus(r.getTicketId(), RequestStatus.IN_PROGRESS));
        assertThrows(DataAccessException.class, () -> service.remove(r.getTicketId()));
        assertThrows(DataAccessException.class, () -> service.processNext());
        store.setDown(false);
        assertEquals(RequestStatus.PENDING, r.getStatus());
        assertEquals(1, service.size());
        assertEquals(1, service.pendingCount());
    }

    @Test
    @DisplayName("C13 Restart: a new service loading the same store sees every record and continues ticket numbers")
    void restartPersistence() throws Exception {
        service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x");
        ServiceRequest second = service.submit("S2", "Ben", ServiceCategory.DOCUMENT, 2, "y");
        service.processNext();
        RequestService restarted = new RequestService(store);
        restarted.reload();
        assertEquals(2, restarted.size());
        assertEquals(RequestStatus.IN_PROGRESS, restarted.get("T0001").getStatus());
        assertEquals(1, restarted.pendingCount());
        assertEquals(second.getTicketId(), restarted.pendingInProcessingOrder().get(0).getTicketId());
        assertEquals("T0003", restarted.submit("S3", "Cy", ServiceCategory.PAYMENT, 1, "z").getTicketId());
    }

    @Test
    @DisplayName("C14 Injection-like text is stored as plain data")
    void injectionLikeText() throws Exception {
        String text = "x'); DROP TABLE service_request; --";
        ServiceRequest r = service.submit("O'Brien-01", "Pat O'Brien", ServiceCategory.DOCUMENT, 2, text);
        ServiceRequest stored = service.lookup(r.getTicketId()).get(0);
        assertEquals(text, stored.getConcern());
        assertEquals("Pat O'Brien", stored.getStudent().getStudentName());
    }

    @Test
    @DisplayName("C15 Row deleted elsewhere: update reports not-found (not a crash) and memory stays consistent")
    void deletedElsewhere() throws Exception {
        ServiceRequest r = service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x");
        store.deleteBehindTheServicesBack(r.getTicketId());
        assertThrows(RequestNotFoundException.class, () -> service.updateStatus(r.getTicketId(), RequestStatus.IN_PROGRESS));
        assertEquals(RequestStatus.PENDING, r.getStatus());
        service.reload();
        assertEquals(0, service.size());
    }

    @Test
    @DisplayName("C16 Repeated refresh gives the same result each time")
    void repeatedRefresh() throws Exception {
        service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x");
        service.submit("S2", "Ben", ServiceCategory.DOCUMENT, 5, "y");
        service.reload();
        String first = service.listSorted().toString() + service.summary();
        for (int i = 0; i < 4; i++) {
            service.reload();
            assertEquals(first, service.listSorted().toString() + service.summary());
        }
    }

    @Test
    @DisplayName("C17 Read-only views")
    void defensiveViews() throws Exception {
        service.submit("S1", "Ana", ServiceCategory.ENROLLMENT, 3, "x");
        assertThrows(UnsupportedOperationException.class, () -> service.listAll().clear());
        assertThrows(UnsupportedOperationException.class, () -> service.filter(null, null).clear());
        assertThrows(UnsupportedOperationException.class, () -> service.summary().countByStatus().clear());
    }
}
