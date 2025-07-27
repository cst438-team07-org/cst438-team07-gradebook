package com.cst438.controller;


import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.SectionRepository;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.LoginDTO;
import com.cst438.service.RegistrarServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.shadow.com.univocity.parsers.common.NormalizedString.toArray;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EnrollmentUpdateUnitTest {

    @Autowired
    private WebTestClient client ;

    // a NOOP mock replaces RegistrarServiceProxy and RabbitMQ messaging
    @MockitoBean
    RegistrarServiceProxy registrarService;

    String loginJWT;

    @Autowired
    EnrollmentRepository enrollmentRepository;
    @Autowired
    SectionRepository sectionRepository;

    @Test
    public void putGradesOK() {
        login("ted@csumb.edu", "ted2025");
        // get roster and current grades
        EnrollmentDTO[] grades = client.get().uri("/sections/1/enrollments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO[].class)
                .returnResult().getResponseBody();

        // should be 2 student, sam@csumb.edu, sam4@csumb.edu. Current grade should be null.
        assertEquals(2, grades.length, "class roster should be of length 2");
        for (EnrollmentDTO dto : grades){
            assertNull(dto.grade(), "current grade should be null." );
        }
       List<EnrollmentDTO> updatedGrades = Arrays.stream(grades)
                .map(dto ->
                     new EnrollmentDTO(
                            dto.enrollmentId(),
                            "C",
                            dto.studentId(),
                            dto.name(),
                            dto.email(),
                            dto.courseId(),
                            dto.title(),
                            dto.sectionId(),
                            dto.sectionNo(),
                            null,
                            null,
                            null,
                            0,
                            0,
                            null
                    )
                ).toList();
        // put grades
        client.put().uri("/enrollments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedGrades)
                .exchange()
                .expectStatus().isOk();

        // verify that database was updated
        for (EnrollmentDTO d : grades) {
            Enrollment enrollment = enrollmentRepository.findById(d.enrollmentId()).orElse(null);
            assertEquals("C", enrollment.getGrade());
        }

        // verify that Registrar messages were sent (one for each Enrollment update)
        verify(registrarService, times(2)).sendMessage(eq("updateEnrollment"), any());

        // fetch grades again and verify updated grades
        grades = client.get().uri("/sections/1/enrollments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO[].class)
                .returnResult().getResponseBody();

        // should be 2 student, sam@csumb.edu, sam4@csumb.edu. Current grade should be null.
        assertEquals(2, grades.length, "class roster should be of length 2");
        for (EnrollmentDTO dto : grades){
            assertEquals("C", dto.grade(), "return grade has incorrect value after update." );
        }
    }

    @Test
    public void putGradesBadIdFails() {
        login("ted@csumb.edu", "ted2025");

        // enrollment id 9999 does not exist
        EnrollmentDTO[] dtolist = new EnrollmentDTO[]{
                new EnrollmentDTO(
                        9999,
                        "A",
                        0,
                        null,
                        "sam@csumb.edu",
                        null,
                        null,
                        0,
                        1,
                        null,
                        null,
                        null,
                        0,
                        0,
                        null
                )
        };

        client.put().uri("/enrollments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dtolist)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void putGradesAnotherInstructorFails() {
        // login as ted2@csumb.edu and get enrollments.
        login("ted2@csumb.edu", "ted2025");
        EnrollmentDTO[] grades = client.get().uri("/sections/6/enrollments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EnrollmentDTO[].class)
                .returnResult().getResponseBody();

        // then login as ted@csumb.edu and attempt to update enrollments
        login("ted@csumb.edu", "ted2025");
        client.put().uri("/enrollments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void getGradesAnotherInstructorFails() {
        // login as ted@csumb.edu,  attempt to get Enrollments for ted1@csumb.edu section_no = 6
        login("ted@csumb.edu", "ted2025");
        client.get().uri("/sections/6/enrollments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void getGradesSectionDoesNotExistFails() {
        login("ted@csumb.edu", "ted2025");
        client.get().uri("/sections/9999/enrollments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    private void login(String email, String password) {
        EntityExchangeResult<LoginDTO> login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        loginJWT = login_dto.getResponseBody().jwt();
        assertNotNull(loginJWT);
    }

}
