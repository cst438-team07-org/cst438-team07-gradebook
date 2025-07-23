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
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {

        String email = principal.getName();

        // ✅ Fetch sections from the repository
        List<Section> sections = sectionRepository.findByInstructorEmailAndYearAndSemester(email, year, semester);

        List<SectionDTO> result = new ArrayList<>();
        for (Section s : sections) {
            // 🔍 Fetch instructor name using email
            User instructor = userRepository.findByEmail(s.getInstructorEmail());
            String instructorName = (instructor != null) ? instructor.getName() : "Unknown";

            // 🎯 Build DTO
            result.add(new SectionDTO(
                    s.getSectionNo(),
                    s.getTerm().getYear(),
                    s.getTerm().getSemester(),
                    s.getCourse().getCourseId(),
                    s.getCourse().getTitle(),
                    s.getSectionId(),
                    s.getBuilding(),
                    s.getRoom(),
                    s.getTimes(),
                    instructorName,
                    s.getInstructorEmail()
            ));
        }

        return result;
    }


    // instructor lists assignments for a section.
    @GetMapping("/sections/{secNo}/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<AssignmentDTO> getAssignments(
            @PathVariable("secNo") int secNo,
            Principal principal) {
        Section section = sectionRepository.findById(secNo).orElse(null);
        if (section == null || !section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized or section not found.");
        }

        List<Assignment> assignments = assignmentRepository.findBySectionSectionNo(secNo);

        List<AssignmentDTO> result = new ArrayList<>();
        for (Assignment a : assignments) {
            result.add(new AssignmentDTO(
                    a.getAssignmentId(),                        // id
                    a.getTitle(),                               // title
                    a.getDueDate().toString(),                  // dueDate
                    a.getSection().getCourse().getCourseId(),   // courseId
                    a.getSection().getSectionId(),              // secId
                    a.getSection().getSectionNo()               // secNo
            ));
        }

        return result;
    }
    // verify that user is the instructor for the section
    //  return list of assignments for the Section


    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO createAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) {
        Section section = sectionRepository.findById(dto.secId()).orElse(null);
        if (section == null || !section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized or section not found.");
        }

        Date due = Date.valueOf(dto.dueDate());
        if (due.before(section.getTerm().getStartDate()) || due.after(section.getTerm().getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date must be within term.");
        }

        Assignment assignment = new Assignment();
        assignment.setTitle(dto.title());
        assignment.setDueDate(due);
        assignment.setSection(section);

        assignment = assignmentRepository.save(assignment);
        return new AssignmentDTO(
                assignment.getAssignmentId(),
                assignment.getTitle(),
                assignment.getDueDate().toString(),
                assignment.getSection().getCourse().getCourseId(),
                assignment.getSection().getSectionId(),
                assignment.getSection().getSectionNo()
        );

    }
    //  user must be the instructor for the Section
    //  check that assignment dueDate is between start date and
    //  end date of the term
    //  create and save an Assignment entity
    //  return AssignmentDTO with database generated primary key


    @PutMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO updateAssignment(@Valid @RequestBody AssignmentDTO dto, Principal principal) {
        Assignment assignment = assignmentRepository.findById(dto.id()).orElse(null);
        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found.");
        }

        if (!assignment.getSection().getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to update this assignment.");
        }

        assignment.setTitle(dto.title());
        assignment.setDueDate(Date.valueOf(dto.dueDate()));
        assignment = assignmentRepository.save(assignment);

        return new AssignmentDTO(
                assignment.getAssignmentId(),
                assignment.getTitle(),
                assignment.getDueDate().toString(),
                assignment.getSection().getCourse().getCourseId(),
                assignment.getSection().getSectionId(),
                assignment.getSection().getSectionNo()
        );
    }

    //  update Assignment Entity.  only title and dueDate fields can be changed.
    //  user must be instructor of the Section


    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void deleteAssignment(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found.");
        }

        if (!assignment.getSection().getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to delete this assignment.");
        }

        assignmentRepository.delete(assignment);
    }
    // verify that user is the instructor of the section
    // delete the Assignment entity


    // student lists their assignments/grades  ordered by due date
    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<AssignmentStudentDTO> getStudentAssignments(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {
        String studentEmail = principal.getName();
        List<Assignment> assignments = assignmentRepository.findByStudentEmailAndYearAndSemester(studentEmail, year, semester);

        List<AssignmentStudentDTO> result = new ArrayList<>();
        for (Assignment a : assignments) {
            Integer score = null;

            for (Grade g : a.getGrades()) {
                if (g.getEnrollment().getStudent().getEmail().equals(studentEmail)) {
                    score = g.getScore(); // ✅ assign score if grade found
                    break;
                }
            }

            AssignmentStudentDTO dto = new AssignmentStudentDTO(
                    a.getAssignmentId(),
                    a.getTitle(),
                    a.getDueDate(),
                    a.getSection().getCourse().getCourseId(),
                    a.getSection().getSectionNo(),
                    score // may be null
            );

            result.add(dto);
        }

        // Optional: sort by due date
        result.sort((a1, a2) -> a1.dueDate().compareTo(a2.dueDate()));

        return result;
    }
    //  return AssignmentStudentDTOs with scores of a
    //  Grade entity exists.
    //  hint: use the GradeRepository findByStudentEmailAndAssignmentId
    //  If assignment has not been graded, return a null score.
}

