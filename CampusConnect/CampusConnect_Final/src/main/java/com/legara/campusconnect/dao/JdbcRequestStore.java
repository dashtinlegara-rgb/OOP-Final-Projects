package com.legara.campusconnect.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.legara.campusconnect.exception.DataAccessException;
import com.legara.campusconnect.model.RequestFactory;
import com.legara.campusconnect.model.RequestStatus;
import com.legara.campusconnect.model.ServiceCategory;
import com.legara.campusconnect.model.ServiceRequest;
import com.legara.campusconnect.model.Student;
import com.legara.campusconnect.service.RequestStore;

public class JdbcRequestStore implements RequestStore {

    private static final String SELECT_ALL =
            "SELECT ticket_id, student_id, student_name, category, priority, concern, status, created_at "
            + "FROM service_request ORDER BY created_at, ticket_id";
    private static final String SELECT_BY_ID =
            "SELECT ticket_id, student_id, student_name, category, priority, concern, status, created_at "
            + "FROM service_request WHERE ticket_id = ?";
    private static final String INSERT =
            "INSERT INTO service_request "
            + "(ticket_id, student_id, student_name, category, priority, concern, estimated_days, status, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_STATUS = "UPDATE service_request SET status = ? WHERE ticket_id = ?";
    private static final String DELETE = "DELETE FROM service_request WHERE ticket_id = ?";

    private final ConnectionFactory connections;

    public JdbcRequestStore(ConnectionFactory connections) {
        if (connections == null) {
            throw new IllegalArgumentException("Connection factory is required.");
        }
        this.connections = connections;
    }

    @Override
    public List<ServiceRequest> findAll() throws DataAccessException {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            List<ServiceRequest> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load requests from " + connections.describe() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public ServiceRequest findById(String ticketId) throws DataAccessException {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BY_ID)) {
            ps.setString(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not look up ticket " + ticketId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void insert(ServiceRequest request) throws DataAccessException {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setString(1, request.getTicketId());
            ps.setString(2, request.getStudent().getStudentId());
            ps.setString(3, request.getStudent().getStudentName());
            ps.setString(4, request.getCategory().name());
            ps.setInt(5, request.getPriority());
            ps.setString(6, request.getConcern());
            ps.setInt(7, request.estimateDays());
            ps.setString(8, request.getStatus().name());
            ps.setTimestamp(9, Timestamp.valueOf(request.getCreatedAt()));
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException("Expected 1 inserted row for " + request.getTicketId() + " but got " + rows + ".");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save ticket " + request.getTicketId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateStatus(String ticketId, RequestStatus status) throws DataAccessException {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status.name());
            ps.setString(2, ticketId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not update ticket " + ticketId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String ticketId) throws DataAccessException {
        try (Connection c = connections.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE)) {
            ps.setString(1, ticketId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete ticket " + ticketId + ": " + e.getMessage(), e);
        }
    }

    private static ServiceRequest mapRow(ResultSet rs) throws SQLException {
        try {
            return RequestFactory.create(
                    ServiceCategory.valueOf(rs.getString("category")),
                    rs.getString("ticket_id"),
                    new Student(rs.getString("student_id"), rs.getString("student_name")),
                    rs.getInt("priority"),
                    rs.getString("concern"),
                    RequestStatus.valueOf(rs.getString("status")),
                    rs.getTimestamp("created_at").toLocalDateTime());
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid data in row " + rs.getString("ticket_id") + ": " + e.getMessage(), e);
        }
    }
}
