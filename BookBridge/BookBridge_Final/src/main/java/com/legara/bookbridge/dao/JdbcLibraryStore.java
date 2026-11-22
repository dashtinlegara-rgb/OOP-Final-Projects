package com.legara.bookbridge.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
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
import com.legara.bookbridge.service.LibraryStore;

public class JdbcLibraryStore implements LibraryStore {
    private final ConnectionFactory connections;

    public JdbcLibraryStore(ConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Member> loadMembers() throws DataAccessException {
        String sql = "SELECT member_id, member_name, membership_type FROM member ORDER BY created_at, member_id";
        List<Member> result = new ArrayList<>();
        try (Connection con = connections.open();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MembershipType type = MembershipType.valueOf(rs.getString("membership_type"));
                result.add(Member.create(type, rs.getString("member_id"), rs.getString("member_name")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load members: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<LibraryBook> loadBooks() throws DataAccessException {
        String sql = "SELECT isbn, title FROM book ORDER BY created_at, isbn";
        List<LibraryBook> result = new ArrayList<>();
        try (Connection con = connections.open();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new LibraryBook(rs.getString("isbn"), rs.getString("title")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load books: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<LoanRecord> loadLoans(Map<String, Member> members, Map<String, LibraryBook> books)
            throws DataAccessException {
        String sql = "SELECT loan_id, member_id, isbn, loan_date, due_date, return_date, status FROM loan ORDER BY loan_id";
        List<LoanRecord> result = new ArrayList<>();
        try (Connection con = connections.open();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date returned = rs.getDate("return_date");
                result.add(new LoanRecord(
                        rs.getString("loan_id"),
                        members.get(rs.getString("member_id")),
                        books.get(rs.getString("isbn")),
                        rs.getDate("loan_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate(),
                        returned == null ? null : returned.toLocalDate(),
                        LoanStatus.valueOf(rs.getString("status"))));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load loans: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<Reservation> loadReservations(Map<String, Member> members, Map<String, LibraryBook> books)
            throws DataAccessException {
        String sql = "SELECT reservation_id, member_id, isbn, reserved_on, status FROM reservation ORDER BY reservation_id";
        List<Reservation> result = new ArrayList<>();
        try (Connection con = connections.open();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Reservation(
                        rs.getString("reservation_id"),
                        members.get(rs.getString("member_id")),
                        books.get(rs.getString("isbn")),
                        rs.getDate("reserved_on").toLocalDate(),
                        ReservationStatus.valueOf(rs.getString("status"))));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load reservations: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public void insertMember(Member member) throws DataAccessException {
        String sql = "INSERT INTO member (member_id, member_name, membership_type) VALUES (?, ?, ?)";
        try (Connection con = connections.open();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, member.getMemberId());
            ps.setString(2, member.getMemberName());
            ps.setString(3, member.getMembershipType().name());
            expectOneRow(ps.executeUpdate(), "insert member " + member.getMemberId());
        } catch (SQLException e) {
            throw new DataAccessException("Could not save member " + member.getMemberId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void insertBook(LibraryBook book) throws DataAccessException {
        String sql = "INSERT INTO book (isbn, title) VALUES (?, ?)";
        try (Connection con = connections.open();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            expectOneRow(ps.executeUpdate(), "insert book " + book.getIsbn());
        } catch (SQLException e) {
            throw new DataAccessException("Could not save book " + book.getIsbn() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void insertLoan(LoanRecord loan, String fulfilledReservationId) throws DataAccessException {
        String insertLoan = "INSERT INTO loan (loan_id, member_id, isbn, loan_date, due_date, status) VALUES (?, ?, ?, ?, ?, ?)";
        String fulfil = "UPDATE reservation SET status = ? WHERE reservation_id = ? AND status = ?";
        try (Connection con = connections.open()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(insertLoan)) {
                ps.setString(1, loan.getLoanId());
                ps.setString(2, loan.getMember().getMemberId());
                ps.setString(3, loan.getBook().getIsbn());
                ps.setDate(4, Date.valueOf(loan.getLoanDate()));
                ps.setDate(5, Date.valueOf(loan.getDueDate()));
                ps.setString(6, LoanStatus.ACTIVE.name());
                expectOneRow(ps.executeUpdate(), "insert loan " + loan.getLoanId());
                if (fulfilledReservationId != null) {
                    try (PreparedStatement ps2 = con.prepareStatement(fulfil)) {
                        ps2.setString(1, ReservationStatus.FULFILLED.name());
                        ps2.setString(2, fulfilledReservationId);
                        ps2.setString(3, ReservationStatus.PENDING.name());
                        expectOneRow(ps2.executeUpdate(), "fulfil reservation " + fulfilledReservationId);
                    }
                }
                con.commit();
            } catch (SQLException | DataAccessException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save loan " + loan.getLoanId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void markLoanReturned(String loanId, LocalDate returnDate) throws DataAccessException {
        String sql = "UPDATE loan SET status = ?, return_date = ? WHERE loan_id = ? AND status = ?";
        try (Connection con = connections.open();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, LoanStatus.RETURNED.name());
            ps.setDate(2, Date.valueOf(returnDate));
            ps.setString(3, loanId);
            ps.setString(4, LoanStatus.ACTIVE.name());
            expectOneRow(ps.executeUpdate(), "return loan " + loanId);
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the return of " + loanId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void updateDueDate(String loanId, LocalDate newDueDate) throws DataAccessException {
        String sql = "UPDATE loan SET due_date = ? WHERE loan_id = ? AND status = ?";
        try (Connection con = connections.open();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(newDueDate));
            ps.setString(2, loanId);
            ps.setString(3, LoanStatus.ACTIVE.name());
            expectOneRow(ps.executeUpdate(), "renew loan " + loanId);
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the renewal of " + loanId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void insertReservation(Reservation reservation) throws DataAccessException {
        String sql = "INSERT INTO reservation (reservation_id, member_id, isbn, reserved_on, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = connections.open();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reservation.getReservationId());
            ps.setString(2, reservation.getMember().getMemberId());
            ps.setString(3, reservation.getBook().getIsbn());
            ps.setDate(4, Date.valueOf(reservation.getReservedOn()));
            ps.setString(5, reservation.getStatus().name());
            expectOneRow(ps.executeUpdate(), "insert reservation " + reservation.getReservationId());
        } catch (SQLException e) {
            throw new DataAccessException("Could not save reservation " + reservation.getReservationId()
                    + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void updateReservationStatus(String reservationId, ReservationStatus status) throws DataAccessException {
        String sql = "UPDATE reservation SET status = ? WHERE reservation_id = ? AND status = ?";
        try (Connection con = connections.open();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, reservationId);
            ps.setString(3, ReservationStatus.PENDING.name());
            expectOneRow(ps.executeUpdate(), "update reservation " + reservationId);
        } catch (SQLException e) {
            throw new DataAccessException("Could not update reservation " + reservationId + ": " + e.getMessage(), e);
        }
    }

    private void expectOneRow(int affectedRows, String action) throws DataAccessException {
        if (affectedRows != 1) {
            throw new DataAccessException("Expected 1 row to change when trying to " + action
                    + ", but " + affectedRows + " changed. The database may have been modified elsewhere; press Refresh.");
        }
    }
}
