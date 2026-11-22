package com.legara.bookbridge.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class LoanRecord implements Renewable {
    private static int nextLoanNumber = 1;

    private final String loanId;
    private final Member member;
    private final LibraryBook book;
    private final LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private LoanStatus status;

    public LoanRecord(Member member, LibraryBook book, LocalDate loanDate, int loanDays) {
        Objects.requireNonNull(member, "Member is required.");
        Objects.requireNonNull(book, "Book is required.");
        Objects.requireNonNull(loanDate, "Loan date is required.");
        if (loanDays < 1) {
            throw new IllegalArgumentException("Loan days must be at least 1.");
        }
        this.loanId = nextLoanId();
        this.member = member;
        this.book = book;
        this.loanDate = loanDate;
        this.dueDate = loanDate.plusDays(loanDays);
        this.status = LoanStatus.ACTIVE;
    }

    public LoanRecord(String loanId, Member member, LibraryBook book, LocalDate loanDate,
            LocalDate dueDate, LocalDate returnDate, LoanStatus status) {
        this.loanId = Objects.requireNonNull(loanId, "Loan ID is required.");
        this.member = Objects.requireNonNull(member, "Member is required.");
        this.book = Objects.requireNonNull(book, "Book is required.");
        this.loanDate = Objects.requireNonNull(loanDate, "Loan date is required.");
        this.dueDate = Objects.requireNonNull(dueDate, "Due date is required.");
        this.status = Objects.requireNonNull(status, "Status is required.");
        this.returnDate = returnDate;
        continueNumberingAfter(loanId);
    }

    private static synchronized String nextLoanId() {
        return String.format("L%04d", nextLoanNumber++);
    }

    private static synchronized void continueNumberingAfter(String loanId) {
        int number = Integer.parseInt(loanId.substring(1));
        if (number >= nextLoanNumber) {
            nextLoanNumber = number + 1;
        }
    }

    public boolean isActive() {
        return status == LoanStatus.ACTIVE;
    }

    public boolean isOverdue(LocalDate today) {
        return isActive() && today.isAfter(dueDate);
    }

    public long daysLate(LocalDate today) {
        LocalDate end = isActive() ? today : returnDate;
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, end));
    }

    public double estimatedFine(LocalDate today) {
        return daysLate(today) * member.getDailyFine();
    }

    public void markReturned(LocalDate returnDate) {
        Objects.requireNonNull(returnDate, "Return date is required.");
        if (!isActive()) {
            throw new IllegalStateException("Loan " + loanId + " is already returned.");
        }
        this.returnDate = returnDate;
        this.status = LoanStatus.RETURNED;
    }

    public boolean canRenew(int additionalDays, LocalDate today) {
        if (additionalDays < 1 || !isActive() || isOverdue(today)) {
            return false;
        }
        LocalDate newDueDate = dueDate.plusDays(additionalDays);
        return ChronoUnit.DAYS.between(loanDate, newDueDate) <= member.getMaximumLoanDays();
    }

    @Override
    public boolean renew(int additionalDays, LocalDate today) {
        if (additionalDays < 1) {
            throw new IllegalArgumentException("Additional days must be at least 1.");
        }
        if (!canRenew(additionalDays, today)) {
            return false;
        }
        dueDate = dueDate.plusDays(additionalDays);
        return true;
    }

    public String getLoanId() { return loanId; }
    public Member getMember() { return member; }
    public LibraryBook getBook() { return book; }
    public LocalDate getLoanDate() { return loanDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public LoanStatus getStatus() { return status; }
}
