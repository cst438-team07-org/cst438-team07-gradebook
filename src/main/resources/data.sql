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
 (8, 'ted', 'ted2@csumb.edu', '$2a$10$YU83ETxvPriw/t2Kd2wO8u8LoKRtl9auX2MsUAtNIIQuKROBvltdy', 'INSTRUCTOR');

insert into course values
('cst336', 'Internet Programming', 4),
('cst334', 'Operating Systems', 4),
('cst363', 'Introduction to Database', 4),
('cst489', 'Software Engineering', 4),
('cst499', 'Capstone', 4),
('cst599', 'Independent Study', 3);

insert into section (section_no, course_id, section_id, term_id, building, room, times, instructor_email) values
(1, 'cst489', 1, 10, '90', 'B104', 'W F 10-11', 'ted@csumb.edu'),
(2, 'cst599', 1, 10, '10', 'B400', 'F 9-10',    'ted@csumb.edu'),
(3, 'cst599', 1,  9, '90', 'B104', 'W F 10-11', 'ted@csumb.edu'),
(4, 'cst599', 1, 11, '90', 'B104', 'W F 10-11', 'ted@csumb.edu'),
(5, 'cst334', 1, 10, '90', 'B100', 'T H 10-11', 'ted@csumb.edu'),
(6, 'cst334', 2, 10, '90', 'B101', 'M W 10-11', 'ted2@csumb.edu');

--                          Spring 2005         Fall 2005
-- sam@csumb.edu            cst599-1            cst489-1, cst599-1
-- sam1@csumb.edu                               cst599-1
-- sam2@csumb.edu                               cst599-1
-- sam3@csumb.edu                               cst599-1
-- sam4@csumb.edu           cst599-1            cst489-1, cst334-1
insert into enrollment (enrollment_id, grade, section_no, user_id) values
(1, null, 2, 4),
(2, null, 2, 5),
(3, null, 2, 6),
(4, null, 1, 2),
(5, null, 2, 2),
(6, null, 3, 2),
(7, null, 1, 7),
(8, null, 5, 7),
(9, null, 3, 7);
