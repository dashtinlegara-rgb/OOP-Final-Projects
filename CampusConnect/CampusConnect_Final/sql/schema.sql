CREATE DATABASE IF NOT EXISTS campusconnect;
USE campusconnect;

DROP TABLE IF EXISTS service_request;

CREATE TABLE service_request (
    ticket_id      VARCHAR(10)  NOT NULL,
    student_id     VARCHAR(20)  NOT NULL,
    student_name   VARCHAR(100) NOT NULL,
    category       VARCHAR(12)  NOT NULL,
    priority       INT          NOT NULL,
    concern        VARCHAR(255) NOT NULL,
    estimated_days INT          NOT NULL,
    status         VARCHAR(12)  NOT NULL DEFAULT 'PENDING',
    created_at     DATETIME     NOT NULL,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (ticket_id),
    CHECK (category IN ('ENROLLMENT', 'DOCUMENT', 'PAYMENT', 'ADVISING')),
    CHECK (priority BETWEEN 1 AND 5),
    CHECK (estimated_days >= 1),
    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CANCELLED'))
);
