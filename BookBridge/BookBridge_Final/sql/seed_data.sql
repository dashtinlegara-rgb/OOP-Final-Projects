USE bookbridge;

INSERT INTO book (isbn, title) VALUES
    ('333', 'Java Basics'),
    ('111', 'Algorithms'),
    ('222', 'Clean Code'),
    ('444', 'Networks'),
    ('555', 'Database Systems');

INSERT INTO member (member_id, member_name, membership_type) VALUES
    ('S1', 'Sam Santos', 'STUDENT'),
    ('S2', 'Ana Reyes', 'STUDENT'),
    ('F1', 'Fay Cruz', 'FACULTY'),
    ('G1', 'Gina Lim', 'GUEST');
