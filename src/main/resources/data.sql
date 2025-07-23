insert into term (term_id, tyear, semester, add_date, add_deadline, drop_deadline, start_date, end_date) values
 (9, 2025, 'Spring', '2024-11-01', '2025-04-30', '2025-04-30', '2025-01-15', '2025-05-17'),
 (10, 2025, 'Fall',  '2025-04-01', '2025-09-30', '2025-09-30', '2025-08-20', '2025-12-17');

insert into user_table (id, name, email, password, type) values
 (1, 'admin', 'admin@csumb.edu', '$2a$10$8cjz47bjbR4Mn8GMg9IZx.vyjhLXR/SKKMSZ9.mP9vpMu0ssKi8GW' , 'ADMIN'),
 (2, 'sam', 'sam@csumb.edu', '$2a$10$B3E9IWa9fCy1SaMzfg1czu312d0xRAk1OU2sw5WOE7hs.SsLqGE9O', 'STUDENT'),
 (3, 'ted', 'ted@csumb.edu', '$2a$10$YU83ETxvPriw/t2Kd2wO8u8LoKRtl9auX2MsUAtNIIQuKROBvltdy', 'INSTRUCTOR');


insert into course values
('cst336', 'Internet Programming', 4),
('cst334', 'Operating Systems', 4),
('cst363', 'Introduction to Database', 4),
('cst489', 'Software Engineering', 4),
('cst499', 'Capstone', 4);

-- 1) One section, taught by ted@csumb.edu, in term #10:
insert into section
(section_no, course_id, section_id, term_id, building, room, times, instructor_email)
values
    (1, 'cst363', 1, 10, 'B1', 'R1', 'TTh 9-10', 'ted@csumb.edu');

-- 2) One assignment with ID=1 in that section:
insert into assignment
(assignment_id, section_no, title, due_date)
values
    (1, 1, 'Lab', '2025-09-15');

-- 3) Create a “Bob” student (so GET /assignments/1/grades produces one record):
insert into user_table
(id, name,        email,             password,       type)
values
    (4, 'Bob', 'bob@csumb.edu', '$2a$10$dummy', 'STUDENT');

-- 4) Enroll Bob in section 1:
insert into enrollment
(enrollment_id, grade, section_no, user_id)
values
    (1, null, 1, 4);

-- 5) Pre–seed a Grade row with ID=1 (so PUT /grades with gradeId 1 will succeed):
insert into grade
(grade_id, enrollment_id, assignment_id, score)
values
    (1, 1, 1, 80);