package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.SectionDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


import java.security.Principal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@RestController
public class AssignmentController {

    private final SectionRepository sectionRepository;
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;

    public AssignmentController(
            SectionRepository sectionRepository,
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository,
            UserRepository userRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
    }

    // get Sections for an instructor
    @GetMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<SectionDTO> getSectionsForInstructor(
            @RequestParam("year") int year ,
            @RequestParam("semester") String semester,
            Principal principal)  {
        // return the Sections that have instructorEmail for the user for the given term.
        List<Section> sections = sectionRepository.findByInstructorEmailAndYearAndSemester(principal.getName(), year, semester);
        return sections.stream().map(s -> {
            User instructor = userRepository.findByEmail(principal.getName());
            return new SectionDTO(
                    s.getSectionNo(),
                    s.getTerm().getYear(),
                    s.getTerm().getSemester(),
                    s.getCourse().getCourseId(),
                    s.getCourse().getTitle(),
                    s.getSectionId(),
                    s.getBuilding(),
                    s.getRoom(),
                    s.getTimes(),
                    instructor.getName(),
                    s.getInstructorEmail());
        }).toList();
    }

    // instructor lists assignments for a section.
    @GetMapping("/sections/{secNo}/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<AssignmentDTO> getAssignments(
            @PathVariable("secNo") int secNo,
            Principal principal) {

        // verify that user is the instructor for the section
        //  return list of assignments
        Section s = sectionRepository.findById(secNo).orElse(null);
        if (s==null || !s.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid section no");
        }
        return s.getAssignments().stream().map(a -> new AssignmentDTO(
                a.getAssignmentId(),
                a.getTitle(),
                a.getDueDate().toString(),
                a.getSection().getCourse().getCourseId(),
                a.getSection().getSectionId(),
                secNo
        )).toList();
    }


    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO createAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) {
        //  create and save an Assignment entity
        //  user must be the instructor for the Section
        //  return AssignmentDTO with database generated primary key
        Section s = sectionRepository.findById(dto.secNo()).orElse(null);
        if (s==null || !s.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid section no");
        }
        Assignment a = new Assignment();
        a.setSection(s);
        a.setTitle(dto.title());
        try {
            Date dueDate = Date.valueOf(dto.dueDate());
            if (dueDate.before(s.getTerm().getStartDate()) || dueDate.after(s.getTerm().getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "due date out of range");
            }
            a.setDueDate(dueDate);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "due data invalid format");
        }
        assignmentRepository.save(a);
        return new AssignmentDTO(
                a.getAssignmentId(),
                a.getTitle(),
                a.getDueDate().toString(),
                a.getSection().getCourse().getCourseId(),
                a.getSection().getSectionId(),
                s.getSectionNo()
        );
    }


    @PutMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO updateAssignment(@Valid @RequestBody AssignmentDTO dto, Principal principal) {
        //  update Assignment Entity.  only title and dueDate fields can be changed.
        //  user must be instructor of the Section
        Assignment a = assignmentRepository.findById(dto.id()).orElse(null);
        if (a==null || !a.getSection().getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid assignment id");
        }
        a.setTitle(dto.title());
        try {
            Date dueDate = Date.valueOf(dto.dueDate());
            if (dueDate.before(a.getSection().getTerm().getStartDate()) || dueDate.after(a.getSection().getTerm().getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "due date out of range");
            }
            a.setDueDate(dueDate);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "due data invalid format");
        }
        assignmentRepository.save(a);
        return new AssignmentDTO(
                a.getAssignmentId(),
                a.getTitle(),
                a.getDueDate().toString(),
                a.getSection().getCourse().getCourseId(),
                a.getSection().getSectionId(),
                a.getSection().getSectionNo()
        );
    }


    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void deleteAssignment(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        // verify that user is the instructor of the section
        // delete the Assignment entity
        Assignment a = assignmentRepository.findById(assignmentId).orElse(null);
        if (a==null || !a.getSection().getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid assignment id");
        }
        assignmentRepository.delete(a);
    }

    // student lists their assignments/grades  ordered by due date
    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<AssignmentStudentDTO> getStudentAssignments(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {

        //  return AssignmentStudentDTOs with scores (if the assignment has been graded)
        //  for the logged in student.  If assignment has not been graded, return a null score.
        List<Assignment> assignments = assignmentRepository.findByStudentEmailAndYearAndSemester(principal.getName(), year, semester);
        return assignments.stream().map(a -> {
            Grade g = gradeRepository.findByStudentEmailAndAssignmentId(principal.getName(), a.getAssignmentId());
            return new AssignmentStudentDTO(
                    a.getAssignmentId(),
                    a.getTitle(),
                    a.getDueDate(),
                    a.getSection().getCourse().getCourseId(),
                    a.getSection().getSectionId(),
                    (g==null) ? null : g.getScore()
                    );
        }).toList();
    }
}
