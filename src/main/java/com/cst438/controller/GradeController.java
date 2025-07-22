package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.GradeDTO;
import jakarta.validation.Valid;
import java.util.Collections;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
public class GradeController {
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;

    public GradeController (
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
    }
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @GetMapping("/assignments/{assignmentId}/grades")
    public List<GradeDTO> getAssignmentGrades(@PathVariable("assignmentId") int assignmentId, Principal principal) {
		// Check that the Section of the assignment belongs to the
		// logged in instructor
        // return a list of GradeDTOs containing student scores for an assignment
        // if a Grade entity does not exist, then create the Grade entity
		// with a null score and return the gradeId.
        // Retrieve the assignment
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assignment not found"));

        // Verify that the logged-in instructor owns the section
        String instructorEmail = principal.getName();
        if (!assignment.getSection().getInstructorEmail().equals(instructorEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Instructor does not own this section");
        }

        // Get all enrollments for the section
        List<Enrollment> enrollments = assignment.getSection().getEnrollments();

        // Create a list of GradeDTOs
        List<GradeDTO> gradeDTOs = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            // Find or create a Grade entity for the student and assignment
            Grade grade = gradeRepository.findByStudentEmailAndAssignmentId(
                    enrollment.getStudent().getEmail(), assignmentId);

            if (grade == null) {
                // Create a new Grade entity with null score
                grade = new Grade();
                grade.setAssignment(assignment);
                grade.setEnrollment(enrollment);
                grade.setScore(null);
                gradeRepository.save(grade);
            }

            // Create GradeDTO
            GradeDTO gradeDTO = new GradeDTO(
                    grade.getGradeId(),
                    enrollment.getStudent().getName(),
                    enrollment.getStudent().getEmail(),
                    assignment.getTitle(),
                    assignment.getSection().getCourse().getCourseId(),
                    assignment.getSection().getSectionId(),
                    grade.getScore()
            );
            gradeDTOs.add(gradeDTO);
        }
        return gradeDTOs;
    }


    @PutMapping("/grades")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public ResponseEntity<?> updateGrades( @RequestBody List<GradeDTO> dtoList, Principal principal) {
		// for each GradeDTO
		// check that the logged in instructor is the owner of the section
        // update the assignment score

        List<String> errors = new ArrayList<>();
        for (int i = 0; i < dtoList.size(); i++) {
            GradeDTO g = dtoList.get(i);
            if (g.score() == null) {
                errors.add("item " + i + ": score must not be null");
            }
            if (g.studentEmail() == null || !g.studentEmail().matches(".+@.+\\..+")) {
                errors.add("item " + i + ": studentEmail must be a valid email");
            }
        }
        if (!errors.isEmpty()) {
            return ResponseEntity
                .badRequest()
                .body(Collections.singletonMap("errors", errors));
        }


        String instructorEmail = principal.getName();

        for (GradeDTO gradeDTO : dtoList) {
            // Retrieve the grade
            Grade grade = gradeRepository.findById(gradeDTO.gradeId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Grade not found"));

            // Verify that the grade's assignment belongs to the instructor's section
            Assignment assignment = grade.getAssignment();
            if (!assignment.getSection().getInstructorEmail().equals(instructorEmail)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Instructor does not own this section");
            }

            // Update the score
            grade.setScore(gradeDTO.score());
            gradeRepository.save(grade);
        }
        return ResponseEntity.ok().build();
    }
}
