package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.service.RegistrarServiceProxy;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.Enrollment;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.security.Principal;
import java.util.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentControllerUnitTest {

  @Mock
  EnrollmentRepository enrollmentRepository;

  @Mock
  SectionRepository sectionRepository;

  @Mock
  RegistrarServiceProxy registrar;

  @Mock
  Principal principal;

  @InjectMocks
  EnrollmentController controller;

  // --- GET /sections/{sectionNo}/enrollments ---

  @Test
  void getEnrollments_success() {
    int sectionNo = 100;
    String instructorEmail = "prof@school.edu";

    // 1) mock and stub Section
    Section sec = mock(Section.class);
    when(sec.getInstructorEmail()).thenReturn(instructorEmail);
    when(sec.getSectionNo()).thenReturn(sectionNo);
    when(sec.getSectionId()).thenReturn(42);
    when(sec.getBuilding()).thenReturn("Bldg");
    when(sec.getRoom()).thenReturn("101");
    when(sec.getTimes()).thenReturn("MWF 9-10");

    Course course = new Course();
    course.setCourseId("CS101");
    course.setTitle("Intro");
    course.setCredits(3);
    when(sec.getCourse()).thenReturn(course);

    Term term = new Term();
    term.setYear(2025);
    term.setSemester("Spring");
    when(sec.getTerm()).thenReturn(term);

    when(sectionRepository.findById(sectionNo))
        .thenReturn(Optional.of(sec));
    when(principal.getName()).thenReturn(instructorEmail);

    // 2) mock and stub Enrollment
    Enrollment e = mock(Enrollment.class);
    when(e.getEnrollmentId()).thenReturn(99);
    when(e.getGrade()).thenReturn("A");
    User stu = new User();
    stu.setId(7);
    stu.setName("Jane Doe");
    stu.setEmail("jane@school.edu");
    when(e.getStudent()).thenReturn(stu);
    when(e.getSection()).thenReturn(sec);

    when(enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(sectionNo))
        .thenReturn(List.of(e));

    // 3) call controller
    List<EnrollmentDTO> dtos = controller.getEnrollments(sectionNo, principal);

    // 4) assertions
    assertNotNull(dtos);
    assertEquals(1, dtos.size());
    EnrollmentDTO dto = dtos.get(0);
    assertEquals(99, dto.enrollmentId());
    assertEquals("A", dto.grade());
    assertEquals(7, dto.studentId());
    assertEquals("Jane Doe", dto.name());
    assertEquals("CS101", dto.courseId());
    assertEquals("Intro", dto.title());
    assertEquals(3, dto.credits());
    assertEquals(42, dto.sectionId());
    assertEquals(sectionNo, dto.sectionNo());
    assertEquals("Bldg", dto.building());
    assertEquals("101", dto.room());
    assertEquals("MWF 9-10", dto.times());
    assertEquals(2025, dto.year());
    assertEquals("Spring", dto.semester());

    verify(enrollmentRepository)
        .findEnrollmentsBySectionNoOrderByStudentName(sectionNo);
    verifyNoInteractions(registrar);
  }


  @Test
  void getEnrollments_sectionNotFound_throws404() {
    when(sectionRepository.findById(42)).thenReturn(Optional.empty());
    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> controller.getEnrollments(42, principal)
    );
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void getEnrollments_notInstructor_throws403() {
    int sectionNo = 200;
    Section section = new Section();
    section.setInstructorEmail("other@school.edu");
    when(sectionRepository.findById(sectionNo)).thenReturn(Optional.of(section));
    when(principal.getName()).thenReturn("prof@school.edu");

    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> controller.getEnrollments(sectionNo, principal)
    );
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  // --- PUT /enrollments ---

  @Test
  void updateEnrollmentGrade_success() {
    int sectionNo = 300;
    int enrollmentId = 55;
    String instructorEmail = "prof@school.edu";

    // stub section lookup
    Section section = new Section();
    section.setInstructorEmail(instructorEmail);
    when(sectionRepository.findById(sectionNo)).thenReturn(Optional.of(section));
    when(principal.getName()).thenReturn(instructorEmail);

    // stub enrollment lookup
    Enrollment enrollment = mock(Enrollment.class);
    when(enrollmentRepository.findById(enrollmentId))
        .thenReturn(Optional.of(enrollment));

    // prepare DTO
    EnrollmentDTO dto = new EnrollmentDTO(
        enrollmentId, "B+", 123, "Stu", "stu@school.edu",
        "CS101", "Intro", 1, sectionNo,
        "Bldg", "101", "MWF 9-10",
        3, 2025, "Spring"
    );

    controller.updateEnrollmentGrade(Collections.singletonList(dto), principal);

    // verify grade update and registrar notification
    verify(enrollment).setGrade("B+");
    verify(enrollmentRepository).save(enrollment);
    verify(registrar).sendMessage("updateEnrollment", dto);
  }

  @Test
  void updateEnrollmentGrade_sectionNotFound_throws404() {
    when(sectionRepository.findById(999)).thenReturn(Optional.empty());
    EnrollmentDTO dto = new EnrollmentDTO(
        1, "A", 0, "", "", "", "", 0,
        999, "", "", "", 0,0,""
    );
    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> controller.updateEnrollmentGrade(Collections.singletonList(dto), principal)
    );
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void updateEnrollmentGrade_notInstructor_throws403() {
    int sectionNo = 400;
    Section section = new Section();
    section.setInstructorEmail("other@school.edu");
    when(sectionRepository.findById(sectionNo)).thenReturn(Optional.of(section));
    when(principal.getName()).thenReturn("prof@school.edu");

    EnrollmentDTO dto = new EnrollmentDTO(
        2, "A", 0, "", "", "", "", 0,
        sectionNo, "", "", "", 0,0,""
    );
    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> controller.updateEnrollmentGrade(Collections.singletonList(dto), principal)
    );
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void updateEnrollmentGrade_enrollmentNotFound_throws404() {
    int sectionNo = 500;
    Section section = new Section();
    section.setInstructorEmail("prof@school.edu");
    when(sectionRepository.findById(sectionNo)).thenReturn(Optional.of(section));
    when(principal.getName()).thenReturn("prof@school.edu");

    when(enrollmentRepository.findById(77)).thenReturn(Optional.empty());

    EnrollmentDTO dto = new EnrollmentDTO(
        77, "C", 0, "", "", "", "", 0,
        sectionNo, "", "", "", 0,0,""
    );
    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> controller.updateEnrollmentGrade(Collections.singletonList(dto), principal)
    );
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }
}
