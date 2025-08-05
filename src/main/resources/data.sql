insert into term (term_id, tyear, semester, add_date, add_deadline, drop_deadline, start_date, end_date) values
 (9, 2025, 'Spring', '2024-11-01', '2025-04-30', '2025-04-30', '2025-01-15', '2025-05-17'),
 (10, 2025, 'Fall',  '2025-04-01', '2025-09-30', '2025-09-30', '2025-08-20', '2025-12-17'),
 (11, 2026, 'Spring', '2025-12-01', '2026-01-30', '2026-02-01', '2026-01-15', '2026-05-17');


insert into user_table (id, name, email, password, type) values
 (1, 'admin', 'admin@csumb.edu', '$2a$10$8cjz47bjbR4Mn8GMg9IZx.vyjhLXR/SKKMSZ9.mP9vpMu0ssKi8GW' , 'ADMIN'),
 (2, 'sam', 'sam@csumb.edu', '$2a$10$B3E9IWa9fCy1SaMzfg1czu312d0xRAk1OU2sw5WOE7hs.SsLqGE9O', 'STUDENT'),
 (3, 'ted', 'ted@csumb.edu', '$2a$10$YU83ETxvPriw/t2Kd2wO8u8LoKRtl9auX2MsUAtNIIQuKROBvltdy', 'INSTRUCTOR'),
 (4, 'sama', 'sam1@csumb.edu', '$2a$10$B3E9IWa9fCy1SaMzfg1czu312d0xRAk1OU2sw5WOE7hs.SsLqGE9O', 'STUDENT'),
 (5, 'samb', 'sam2@csumb.edu', '$2a$10$B3E9IWa9fCy1SaMzfg1czu312d0xRAk1OU2sw5WOE7hs.SsLqGE9O', 'STUDENT'),
 (6, 'samc', 'sam3@csumb.edu', '$2a$10$B3E9IWa9fCy1SaMzfg1czu312d0xRAk1OU2sw5WOE7hs.SsLqGE9O', 'STUDENT'),
 (7, 'sam',  'sam4@csumb.edu', '$2a$10$B3E9IWa9fCy1SaMzfg1czu312d0xRAk1OU2sw5WOE7hs.SsLqGE9O', 'STUDENT'),
 (8, 'ted', 'ted2@csumb.edu', '$2a$10$YU83ETxvPriw/t2Kd2wO8u8LoKRtl9auX2MsUAtNIIQuKROBvltdy', 'INSTRUCTOR'),
 (9, 'sally', 'sally@csumb.edu', '$2a$10$LbnNBkctBxuHlZDdaKMW6OPIFkPHvKJ/8WaT4ZtA6KdR/ANm/dA0O', 'INSTRUCTOR'),
 (10, 'bob', 'bob@csumb.edu', '$2a$10$/CdPw/L6tWg0bNewmIFgmO2k2sZpVbYFYzvLZeT5b2o.xX2Pt78RK', 'STUDENT');


insert into course values
                       ('cst336', 'Internet Programming', 4),
                       ('cst334', 'Operating Systems', 4),
                       ('cst363', 'Introduction to Database', 4),
                       ('cst489', 'Software Engineering', 4),
                       ('cst499', 'Capstone', 4),
                       ('cst599', 'AI Development', 4),
                       ('cst393', 'Software Engineering Project', 4),
                       ('cst328', 'Digital art and design', 4),
                       ('cst370', 'Design and analysis algorithms', 4),
                       ('cst383', 'Introduction to data science', 4);

-- 1) One section, taught by ted@csumb.edu, in term #10:
insert into section (section_no, course_id, section_id, term_id, building, room, times, instructor_email) values
                                                                                                              (1, 'cst489', 1, 10, '90', 'B104', 'W F 10-11', 'ted@csumb.edu'),
                                                                                                              (2, 'cst599', 1, 10, '10', 'B400', 'F 9-10',    'ted@csumb.edu'),
                                                                                                              (3, 'cst599', 1,  9, '90', 'B104', 'W F 10-11', 'ted@csumb.edu'),
                                                                                                              (4, 'cst599', 1, 11, '90', 'B104', 'W F 10-11', 'ted@csumb.edu'),
                                                                                                              (5, 'cst334', 1, 10, '90', 'B100', 'T H 10-11', 'ted@csumb.edu'),
                                                                                                              (6, 'cst334', 2, 10, '90', 'B101', 'M W 10-11', 'ted2@csumb.edu');

-- add open sections for Fall 2025
insert into section (section_no, course_id, section_id, term_id, building, room, times, instructor_email) values
                                                                                                              (7, 'cst336',  2, 10, '90',  'B105', 'M W 9-10',  'ted@csumb.edu'),
                                                                                                              (8, 'cst499',  3, 10, '90',  'B106', 'T Th 11-12','sally@csumb.edu'),
                                                                                                              (9, 'cst393', 10, 10, '92', 'B107', 'M W 2-4', 'sally@csumb.edu'),
                                                                                                              (10, 'cst370', 11, 10, '95', 'B110', 'F 8-10', 'ted@csumb.edu');

-- add some open sections for Spring 2025 too
insert into section (section_no, course_id, section_id, term_id, building, room, times, instructor_email) values
                                                                                                              (12, 'cst334',  4, 9, '01',  '201', 'T Th 1-2',   'ted@csumb.edu'),
                                                                                                              (13, 'cst363',  5, 9, '01',  '202', 'M W 10-11', 'ted@csumb.edu'),
                                                                                                              (14, 'cst328',  6, 9, '01',  '203', 'T F 6-9', 'ted@csumb.edu'),
                                                                                                              (15, 'cst370',  7, 9, '01',  '205', 'M TH 1-4', 'sally@csumb.edu'),
                                                                                                              (16, 'cst383',  8, 9, '01',  '208', 'W F 9-11', 'sally@csumb.edu'),
                                                                                                              (17, 'cst393',  9, 9, '01',  '209', 'T TH 2-6', 'sally@csumb.edu');


-- enroll soma to cst599 Fall 2025
INSERT INTO enrollment (enrollment_id, grade, section_no, user_id) VALUES (10000, NULL, 2, 4),
                                                                          (10002, null, 2, 5),
                                                                          (10003, null, 2, 6),
                                                                          (10004, null, 1, 2),
                                                                          (10005, null, 2, 2),
                                                                          (10006, null, 3, 2),
                                                                          (10007, null, 1, 7),
                                                                          (10008, null, 5, 7),
                                                                          (10009, null, 3, 7);



-- 2) One assignment with ID=1 in that section:
insert into assignment
(assignment_id, section_no, title, due_date)
values
    (1, 1, 'Lab', '2025-09-15');


