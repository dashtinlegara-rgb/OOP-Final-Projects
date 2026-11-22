package com.legara.campusconnect.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.legara.campusconnect.exception.DataAccessException;
import com.legara.campusconnect.exception.DuplicateTicketException;
import com.legara.campusconnect.exception.InvalidRequestException;
import com.legara.campusconnect.exception.InvalidStatusTransitionException;
import com.legara.campusconnect.exception.RequestNotFoundException;
import com.legara.campusconnect.model.RequestFactory;
import com.legara.campusconnect.model.RequestStatus;
import com.legara.campusconnect.model.ServiceCategory;
import com.legara.campusconnect.model.ServiceRequest;
import com.legara.campusconnect.model.Student;

public class RequestService {

    public static final Comparator<ServiceRequest> DISPLAY_ORDER = Comparator
            .comparingInt(ServiceRequest::getPriority).reversed()
            .thenComparingInt(ServiceRequest::estimateDays)
            .thenComparing(ServiceRequest::getTicketId);

    public static final Comparator<ServiceRequest> QUEUE_ORDER = Comparator
            .comparingInt(ServiceRequest::getPriority).reversed()
            .thenComparing(ServiceRequest::getCreatedAt)
            .thenComparing(ServiceRequest::getTicketId);

    private final RequestStore store;
    private final Repository<ServiceRequest, String> requests = new Repository<>();
    private final Queue<ServiceRequest> pendingQueue = new LinkedList<>();
    private final Set<String> openStudentCategories = new HashSet<>();
    private int nextTicketNumber = 1;

    public RequestService(RequestStore store) {
        if (store == null) {
            throw new IllegalArgumentException("Store is required.");
        }
        this.store = store;
    }

    public synchronized void reload() throws DataAccessException {
        List<ServiceRequest> loaded = store.findAll();
        requests.clear();
        pendingQueue.clear();
        openStudentCategories.clear();
        int highest = 0;
        for (ServiceRequest r : loaded) {
            requests.put(r.getTicketId(), r);
            if (r.getStatus() == RequestStatus.PENDING) {
                pendingQueue.add(r);
            }
            if (r.isOpen()) {
                openStudentCategories.add(openKey(r));
            }
            highest = Math.max(highest, ticketNumber(r.getTicketId()));
        }
        nextTicketNumber = highest + 1;
    }

    public synchronized ServiceRequest submit(String studentId, String studentName, ServiceCategory category,
            int priority, String concern) throws InvalidRequestException, DuplicateTicketException, DataAccessException {
        String ticketId = candidateTicketId();
        ServiceRequest request = add(ticketId, studentId, studentName, category, priority, concern);
        nextTicketNumber = ticketNumber(ticketId) + 1;
        return request;
    }

    public synchronized ServiceRequest add(String ticketId, String studentId, String studentName,
            ServiceCategory category, int priority, String concern)
            throws InvalidRequestException, DuplicateTicketException, DataAccessException {
        if (category == null) {
            throw new InvalidRequestException("Category is required.");
        }
        ServiceRequest request;
        try {
            Student student = new Student(studentId, studentName);
            request = RequestFactory.create(category, ticketId, student, priority, concern,
                    RequestStatus.PENDING, LocalDateTime.now().withNano(0));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid request: " + e.getMessage(), e);
        }
        if (requests.contains(request.getTicketId())) {
            throw new DuplicateTicketException("Ticket ID already exists: " + request.getTicketId());
        }
        String key = openKey(request);
        if (openStudentCategories.contains(key)) {
            throw new InvalidRequestException(request.getStudent().getStudentName() + " already has an open "
                    + category + " request; resolve or cancel it first.");
        }
        store.insert(request);
        requests.put(request.getTicketId(), request);
        pendingQueue.add(request);
        openStudentCategories.add(key);
        return request;
    }

    public synchronized ServiceRequest get(String ticketId) throws RequestNotFoundException {
        ServiceRequest request = requests.get(ticketId == null ? null : ticketId.trim());
        if (request == null) {
            throw new RequestNotFoundException("No request with ticket ID: " + ticketId);
        }
        return request;
    }

    public List<ServiceRequest> lookup(String ticketId) throws DataAccessException {
        List<ServiceRequest> result = new ArrayList<>();
        String id = ticketId == null ? "" : ticketId.trim();
        if (!id.isEmpty()) {
            ServiceRequest found = store.findById(id);
            if (found != null) {
                result.add(found);
            }
        }
        return result;
    }

    public synchronized ServiceRequest updateStatus(String ticketId, RequestStatus newStatus)
            throws RequestNotFoundException, InvalidStatusTransitionException, DataAccessException {
        ServiceRequest request = get(ticketId);
        RequestStatus current = request.getStatus();
        if (!current.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException("Cannot change " + request.getTicketId() + " from "
                    + current + " to " + newStatus + ".");
        }
        if (!store.updateStatus(request.getTicketId(), newStatus)) {
            throw new RequestNotFoundException("Ticket " + request.getTicketId()
                    + " no longer exists in the database; press Refresh.");
        }
        if (current == RequestStatus.PENDING) {
            pendingQueue.remove(request);
        }
        request.changeStatus(newStatus);
        if (!newStatus.isOpen()) {
            openStudentCategories.remove(openKey(request));
        }
        return request;
    }

    public synchronized ServiceRequest remove(String ticketId) throws RequestNotFoundException, DataAccessException {
        ServiceRequest request = get(ticketId);
        if (!store.delete(request.getTicketId())) {
            throw new RequestNotFoundException("Ticket " + request.getTicketId()
                    + " no longer exists in the database; press Refresh.");
        }
        requests.remove(request.getTicketId());
        pendingQueue.remove(request);
        if (request.isOpen()) {
            openStudentCategories.remove(openKey(request));
        }
        return request;
    }

    public synchronized ServiceRequest processNext() throws RequestNotFoundException, DataAccessException {
        ServiceRequest next = findNextPending();
        if (next == null) {
            throw new RequestNotFoundException("No pending requests to process.");
        }
        if (!store.updateStatus(next.getTicketId(), RequestStatus.IN_PROGRESS)) {
            throw new RequestNotFoundException("Ticket " + next.getTicketId()
                    + " no longer exists in the database; press Refresh.");
        }
        next.changeStatus(RequestStatus.IN_PROGRESS);
        pendingQueue.remove(next);
        return next;
    }

    public synchronized List<ServiceRequest> listAll() {
        return requests.findAll();
    }

    public synchronized List<ServiceRequest> listSorted() {
        return sorted(requests.findAll(), DISPLAY_ORDER);
    }

    public synchronized List<ServiceRequest> pendingInProcessingOrder() {
        return sorted(pendingQueue, QUEUE_ORDER);
    }

    public synchronized List<ServiceRequest> searchByStudent(String studentId) {
        String id = studentId == null ? "" : studentId.trim();
        List<ServiceRequest> result = new ArrayList<>();
        for (ServiceRequest r : requests.findAll()) {
            if (r.getStudent().getStudentId().equals(id)) {
                result.add(r);
            }
        }
        return sorted(result, DISPLAY_ORDER);
    }

    public synchronized List<ServiceRequest> filter(ServiceCategory category, RequestStatus status) {
        List<ServiceRequest> result = new ArrayList<>();
        for (ServiceRequest r : requests.findAll()) {
            boolean categoryMatches = category == null || r.getCategory() == category;
            boolean statusMatches = status == null || r.getStatus() == status;
            if (categoryMatches && statusMatches) {
                result.add(r);
            }
        }
        return sorted(result, DISPLAY_ORDER);
    }

    public synchronized RequestSummary summary() {
        Map<ServiceCategory, Integer> byCategory = new HashMap<>();
        for (ServiceCategory c : ServiceCategory.values()) {
            byCategory.put(c, 0);
        }
        Map<RequestStatus, Integer> byStatus = new HashMap<>();
        for (RequestStatus s : RequestStatus.values()) {
            byStatus.put(s, 0);
        }
        int totalDays = 0;
        List<ServiceRequest> all = requests.findAll();
        for (ServiceRequest r : all) {
            byCategory.put(r.getCategory(), byCategory.get(r.getCategory()) + 1);
            byStatus.put(r.getStatus(), byStatus.get(r.getStatus()) + 1);
            totalDays += r.estimateDays();
        }
        double average = all.isEmpty() ? 0.0 : (double) totalDays / all.size();
        return new RequestSummary(Collections.unmodifiableMap(byCategory), Collections.unmodifiableMap(byStatus),
                average, all.size(), pendingQueue.size());
    }

    public synchronized int size() {
        return requests.size();
    }

    public synchronized int pendingCount() {
        return pendingQueue.size();
    }

    public synchronized boolean hasOpenRequest(String studentId, ServiceCategory category) {
        return openStudentCategories.contains(studentId + "#" + category);
    }

    private ServiceRequest findNextPending() {
        ServiceRequest best = null;
        for (ServiceRequest r : pendingQueue) {
            if (best == null || QUEUE_ORDER.compare(r, best) < 0) {
                best = r;
            }
        }
        return best;
    }

    private static List<ServiceRequest> sorted(Iterable<ServiceRequest> source, Comparator<ServiceRequest> order) {
        List<ServiceRequest> list = new ArrayList<>();
        for (ServiceRequest r : source) {
            list.add(r);
        }
        list.sort(order);
        return Collections.unmodifiableList(list);
    }

    private String candidateTicketId() {
        int number = nextTicketNumber;
        while (requests.contains(formatTicketId(number))) {
            number++;
        }
        return formatTicketId(number);
    }

    private static String formatTicketId(int number) {
        return String.format("T%04d", number);
    }

    private static int ticketNumber(String ticketId) {
        if (ticketId == null || !ticketId.startsWith("T")) {
            return 0;
        }
        try {
            return Integer.parseInt(ticketId.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String openKey(ServiceRequest request) {
        return request.getStudent().getStudentId() + "#" + request.getCategory();
    }
}
