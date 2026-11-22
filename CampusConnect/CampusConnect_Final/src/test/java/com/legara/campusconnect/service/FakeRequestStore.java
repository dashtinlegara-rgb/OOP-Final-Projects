package com.legara.campusconnect.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.legara.campusconnect.exception.DataAccessException;
import com.legara.campusconnect.model.RequestFactory;
import com.legara.campusconnect.model.RequestStatus;
import com.legara.campusconnect.model.ServiceRequest;

class FakeRequestStore implements RequestStore {
    private final Map<String, ServiceRequest> rows = new LinkedHashMap<>();
    private boolean down;

    void setDown(boolean down) {
        this.down = down;
    }

    int rowCount() {
        return rows.size();
    }

    RequestStatus storedStatus(String ticketId) {
        return rows.get(ticketId).getStatus();
    }

    void deleteBehindTheServicesBack(String ticketId) {
        rows.remove(ticketId);
    }

    private void checkUp() throws DataAccessException {
        if (down) {
            throw new DataAccessException("Simulated outage", new SQLException("Communications link failure"));
        }
    }

    private static ServiceRequest copy(ServiceRequest r, RequestStatus status) {
        return RequestFactory.create(r.getCategory(), r.getTicketId(), r.getStudent(), r.getPriority(),
                r.getConcern(), status, r.getCreatedAt());
    }

    @Override
    public List<ServiceRequest> findAll() throws DataAccessException {
        checkUp();
        List<ServiceRequest> result = new ArrayList<>();
        for (ServiceRequest r : rows.values()) {
            result.add(copy(r, r.getStatus()));
        }
        return result;
    }

    @Override
    public ServiceRequest findById(String ticketId) throws DataAccessException {
        checkUp();
        ServiceRequest r = rows.get(ticketId);
        return r == null ? null : copy(r, r.getStatus());
    }

    @Override
    public void insert(ServiceRequest request) throws DataAccessException {
        checkUp();
        if (rows.containsKey(request.getTicketId())) {
            throw new DataAccessException("Duplicate key", new SQLException("Duplicate entry"));
        }
        rows.put(request.getTicketId(), copy(request, request.getStatus()));
    }

    @Override
    public boolean updateStatus(String ticketId, RequestStatus status) throws DataAccessException {
        checkUp();
        ServiceRequest existing = rows.get(ticketId);
        if (existing == null) {
            return false;
        }
        rows.put(ticketId, copy(existing, status));
        return true;
    }

    @Override
    public boolean delete(String ticketId) throws DataAccessException {
        checkUp();
        return rows.remove(ticketId) != null;
    }
}
