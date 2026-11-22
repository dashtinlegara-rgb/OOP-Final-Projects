package com.legara.bookbridge.model;

public class LibraryBook {
    private final String isbn;
    private final String title;
    private BookStatus status;

    public LibraryBook(String isbn, String title) {
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN cannot be blank.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be blank.");
        }
        this.isbn = isbn.trim();
        this.title = title.trim();
        this.status = BookStatus.AVAILABLE;
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public BookStatus getStatus() { return status; }
    public boolean isAvailable() { return status == BookStatus.AVAILABLE; }

    public void markBorrowed() { status = BookStatus.CHECKED_OUT; }
    public void markReturned() { status = BookStatus.AVAILABLE; }
}
