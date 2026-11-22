package com.legara.campusconnect.service;

import java.util.List;

import com.legara.campusconnect.exception.DataAccessException;
import com.legara.campusconnect.model.RequestStatus;
import com.legara.campusconnect.model.ServiceRequest;

public interface RequestStore {
    List<ServiceRequest> findAll() throws DataAccessException;

    ServiceRequest findById(String ticketId) throws DataAccessException;

    void insert(ServiceRequest request) throws DataAccessException;

    boolean updateStatus(String ticketId, RequestStatus status) throws DataAccessException;

    boolean delete(String ticketId) throws DataAccessException;
}
