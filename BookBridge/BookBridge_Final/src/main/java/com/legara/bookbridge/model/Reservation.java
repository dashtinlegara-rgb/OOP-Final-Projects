package com.legara.bookbridge.model;

import java.time.LocalDate;
import java.util.Objects;

public class Reservation {
    private static int nextReservationNumber = 1;

    private final String reservationId;
    private final Member member;
    private final LibraryBook book;
    private final LocalDate reservedOn;
    private ReservationStatus status;

    public Reservation(Member member, LibraryBook book, LocalDate reservedOn) {
        this.member = Objects.requireNonNull(member, "Member is required.");
        this.book = Objects.requireNonNull(book, "Book is required.");
        this.reservedOn = Objects.requireNonNull(reservedOn, "Reservation date is required.");
        this.reservationId = nextReservationId();
        this.status = ReservationStatus.PENDING;
    }

    public Reservation(String reservationId, Member member, LibraryBook book, LocalDate reservedOn,
            ReservationStatus status) {
        this.reservationId = Objects.requireNonNull(reservationId, "Reservation ID is required.");
        this.member = Objects.requireNonNull(member, "Member is required.");
        this.book = Objects.requireNonNull(book, "Book is required.");
        this.reservedOn = Objects.requireNonNull(reservedOn, "Reservation date is required.");
        this.status = Objects.requireNonNull(status, "Status is required.");
        continueNumberingAfter(reservationId);
    }

    private static synchronized String nextReservationId() {
        return String.format("R%04d", nextReservationNumber++);
    }

    private static synchronized void continueNumberingAfter(String reservationId) {
        int number = Integer.parseInt(reservationId.substring(1));
        if (number >= nextReservationNumber) {
            nextReservationNumber = number + 1;
        }
    }

    public boolean isPending() {
        return status == ReservationStatus.PENDING;
    }

    public void markFulfilled() {
        requirePending();
        status = ReservationStatus.FULFILLED;
    }

    public void markCancelled() {
        requirePending();
        status = ReservationStatus.CANCELLED;
    }

    private void requirePending() {
        if (!isPending()) {
            throw new IllegalStateException("Reservation " + reservationId + " is already " + status + ".");
        }
    }

    public String getReservationId() { return reservationId; }
    public Member getMember() { return member; }
    public LibraryBook getBook() { return book; }
    public LocalDate getReservedOn() { return reservedOn; }
    public ReservationStatus getStatus() { return status; }
}
