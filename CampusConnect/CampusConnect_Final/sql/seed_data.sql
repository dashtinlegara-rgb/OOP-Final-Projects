USE campusconnect;

INSERT INTO service_request
    (ticket_id, student_id, student_name, category, priority, concern, estimated_days, status, created_at)
VALUES
    ('T0001', '2023-0001', 'Ana Cruz', 'ENROLLMENT', 3, 'Add a subject to my load', 3, 'PENDING', '2026-11-20 09:00:00'),
    ('T0002', '2023-0002', 'Ben Reyes', 'DOCUMENT', 5, 'Request transcript of records', 1, 'IN_PROGRESS', '2026-11-20 09:05:00'),
    ('T0003', '2023-0003', 'Carla Santos', 'PAYMENT', 5, 'Payment not posted', 1, 'RESOLVED', '2026-11-20 09:10:00'),
    ('T0004', '2023-0004', 'Dan Lim', 'ADVISING', 2, 'Choosing electives', 4, 'PENDING', '2026-11-20 09:15:00'),
    ('T0005', '2023-0005', 'Eve Tan', 'ADVISING', 5, 'Thesis adviser concern', 3, 'PENDING', '2026-11-20 09:20:00');
