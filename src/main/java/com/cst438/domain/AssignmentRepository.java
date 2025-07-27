package com.cst438.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface AssignmentRepository extends CrudRepository<Assignment, Integer> {

    @Query("select a from Assignment a join a.section.term t join a.section.enrollments e where e.student.email=:email and t.year=:year and t.semester=:semester order by a.dueDate")
    List<Assignment> findByStudentEmailAndYearAndSemester(String email, int year, String semester);

    List<Assignment> findBySectionSectionNo(int secNo);
}
