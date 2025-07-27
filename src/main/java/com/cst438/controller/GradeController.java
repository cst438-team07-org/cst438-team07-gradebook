package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.GradeDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
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
        // return a list of GradeDTOs containing student scores for an assignment
        // the user must the instructor for the assignment's section
        // if a Grade entity does not exist, then create the Grade entity with a null score.
        Assignment a = assignmentRepository.findById(assignmentId).orElse(null);
        if (a==null || !a.getSection().getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid assignment id");
        }
        return a.getSection().getEnrollments().stream().map(e -> {
            Grade g = gradeRepository.findByStudentEmailAndAssignmentId(e.getStudent().getEmail(), a.getAssignmentId());
            if (g==null) {
                // if this is the first time assignment is being graded, create Grade with null score
                g = new Grade();
                g.setAssignment(a);
                g.setEnrollment(e);
                g.setScore(null);
                gradeRepository.save(g);
            }
            return new GradeDTO(
                    g.getGradeId(),
                    e.getStudent().getName(),
                    e.getStudent().getEmail(),
                    a.getTitle(),
                    a.getSection().getCourse().getCourseId(),
                    a.getSection().getSectionId(),
                    g.getScore());
        }).toList();
    }


    @PutMapping("/grades")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void updateGrades(@Valid @RequestBody List<GradeDTO> dtoList, Principal principal) {
        // update the assignment score
        // the user must be the instructor for the assignment's section
        for (GradeDTO dto : dtoList ) {
            Grade g = gradeRepository.findById(dto.gradeId()).orElse(null);
            if (g==null || !g.getAssignment().getSection().getInstructorEmail().equals(principal.getName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid grade id "+dto.gradeId());
            }
            g.setScore(dto.score());
            gradeRepository.save(g);
        }
    }
}
