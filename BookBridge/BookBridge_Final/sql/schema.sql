CREATE DATABASE IF NOT EXISTS bookbridge;
USE bookbridge;

DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS loan;
DROP TABLE IF EXISTS book;
DROP TABLE IF EXISTS member;

CREATE TABLE member (
    member_id       VARCHAR(20)  NOT NULL,
    member_name     VARCHAR(100) NOT NULL,
    membership_type VARCHAR(10)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_member PRIMARY KEY (member_id),
    CONSTRAINT chk_member_id CHECK (CHAR_LENGTH(TRIM(member_id)) > 0),
    CONSTRAINT chk_member_name CHECK (CHAR_LENGTH(TRIM(member_name)) > 0),
    CONSTRAINT chk_member_type CHECK (membership_type IN ('STUDENT', 'FACULTY', 'GUEST'))
);

CREATE TABLE book (
    isbn       VARCHAR(20)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_book PRIMARY KEY (isbn),
    CONSTRAINT chk_book_isbn CHECK (CHAR_LENGTH(TRIM(isbn)) > 0),
    CONSTRAINT chk_book_title CHECK (CHAR_LENGTH(TRIM(title)) > 0)
);

CREATE TABLE loan (
    loan_id     VARCHAR(10) NOT NULL,
    member_id   VARCHAR(20) NOT NULL,
    isbn        VARCHAR(20) NOT NULL,
    loan_date   DATE        NOT NULL,
    due_date    DATE        NOT NULL,
    return_date DATE        NULL,
    status      VARCHAR(10) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_loan PRIMARY KEY (loan_id),
    CONSTRAINT fk_loan_member FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT fk_loan_book FOREIGN KEY (isbn) REFERENCES book (isbn),
    CONSTRAINT chk_loan_status CHECK (status IN ('ACTIVE', 'RETURNED')),
    CONSTRAINT chk_loan_dates CHECK (due_date > loan_date),
    CONSTRAINT chk_loan_return CHECK ((status = 'ACTIVE' AND return_date IS NULL)
                                   OR (status = 'RETURNED' AND return_date IS NOT NULL AND return_date >= loan_date))
);

CREATE TABLE reservation (
    reservation_id VARCHAR(10) NOT NULL,
    member_id      VARCHAR(20) NOT NULL,
    isbn           VARCHAR(20) NOT NULL,
    reserved_on    DATE        NOT NULL,
    status         VARCHAR(10) NOT NULL,
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reservation PRIMARY KEY (reservation_id),
    CONSTRAINT fk_reservation_member FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT fk_reservation_book FOREIGN KEY (isbn) REFERENCES book (isbn),
    CONSTRAINT chk_reservation_status CHECK (status IN ('PENDING', 'FULFILLED', 'CANCELLED'))
);
