-- admin@ticketing.com / Admin123!
INSERT INTO users (id, email, password_hash, roles) VALUES
    ('00000000-0000-0000-0000-000000000001',
     'admin@ticketing.com',
     '$2a$12$JJAezPvEdTsIk7p1.DdkIuYTK6DqUoW1bZqm9aQOffhag9M9A5usy',
     'ADMIN');

-- organizer@ticketing.com / Organizer123!
INSERT INTO users (id, email, password_hash, roles) VALUES
    ('00000000-0000-0000-0000-000000000002',
     'organizer@ticketing.com',
     '$2a$12$1ZeH7HW/UjzxM8bytRfnTOvseAq2oR4O2p.w2AXjyxlLs3S8jBKLa',
     'ORGANIZER');

-- customer@ticketing.com / Customer123!
INSERT INTO users (id, email, password_hash, roles) VALUES
    ('00000000-0000-0000-0000-000000000003',
     'customer@ticketing.com',
     '$2a$12$yjevQOObHOIiWwKFKETl1u972rtMtrxKF1s.lcBsGzUJCH5CqVDQi',
     'CUSTOMER');