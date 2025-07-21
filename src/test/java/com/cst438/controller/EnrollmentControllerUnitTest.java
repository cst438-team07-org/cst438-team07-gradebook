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
class EnrollmentControllerTest {

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

    // stub section lookup
    Section section = new Section();
    section.setInstructorEmail(instructorEmail);
    when(sectionRepository.findById(sectionNo)).thenReturn(Optional.of(section));
    when(principal.getName()).thenReturn(instructorEmail);

    // stub enrollments
    Enrollment e = mock(Enrollment.class);
    when(enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(sectionNo))
        .thenReturn(Collections.singletonList(e));

    List<EnrollmentDTO> dtos = controller.getEnrollments(sectionNo, principal);

    assertEquals(1, dtos.size());
    verify(enrollmentRepository).findEnrollmentsBySectionNoOrderByStudentName(sectionNo);
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
