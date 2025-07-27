package com.cst438.controller2;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.LoginDTO;
import com.cst438.service.RegistrarServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AssignmentUpdateDeleteFailUnitTest {

    @Autowired
    private WebTestClient client ;

    // a NOOP mock replaces RegistrarServiceProxy and RabbitMQ messaging
    @MockitoBean
    RegistrarServiceProxy registrarService;

    String loginJWT;

    @Autowired
    AssignmentRepository assignmentRepository;
    @Autowired
    SectionRepository sectionRepository;

    @Test
    public void deleteAssignmentNotExists() {
        login("ted@csumb.edu", "ted2025");
        client.delete().uri("/assignment/9999")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void deleteAssignmentByAnotherInstructor() {
        // ted2@csumb.edu teaches course cst334-2 in term 10
        // create an assignment in ted2's course which is section # 6
        Section s = sectionRepository.findById(6).orElse(null);
        Assignment a = new Assignment();
        a.setTitle("assignment deleteAssignmentByAnotherInstructor");
        a.setDueDate(Date.valueOf("2025-09-01"));
        a.setSection(s);
        assignmentRepository.save(a);

        // login as ted@csumb.edu and try to delte ted2's assignment
        login("ted@csumb.edu", "ted2025");
        client.delete().uri("/assignment/"+a.getAssignmentId())
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void updateAssignmentNotExists() {
        // create assignment for section with ted2@csumb.edu as instructor
        Section s = sectionRepository.findById(6).orElse(null);
        Assignment a = new Assignment();
        a.setTitle("assignment updateAssignmentByAnotherInstructor");
        a.setDueDate(Date.valueOf("2025-09-01"));
        a.setSection(s);
        assignmentRepository.save(a);

        login("ted@csumb.edu", "ted2025");
        AssignmentDTO dto = new AssignmentDTO(99999, a.getTitle(), a.getDueDate().toString(), s.getCourse().getCourseId(), s.getSectionId(), s.getSectionNo() );
        client.put().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void updateAssignmentByAnotherInstructor() {
        // create assignment for section with ted2@csumb.edu as instructor
        Section s = sectionRepository.findById(6).orElse(null);
        Assignment a = new Assignment();
        a.setTitle("assignment updateAssignmentByAnotherInstructor");
        a.setDueDate(Date.valueOf("2025-09-01"));
        a.setSection(s);
        assignmentRepository.save(a);

        // login as ted and attempt to delete assignment from ted2 should fail
        login("ted@csumb.edu", "ted2025");
        AssignmentDTO dto = new AssignmentDTO(a.getAssignmentId(), a.getTitle(), a.getDueDate().toString(), s.getCourse().getCourseId(), s.getSectionId(), s.getSectionNo() );
        client.put().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void updateAssignmentBadDueDate() {
        // create assignment for ted@csumb.edu for course cst334-1
        Section s = sectionRepository.findById(5).orElse(null);
        Assignment a = new Assignment();
        a.setTitle("assignment updateAssignmentByAnotherInstructor");
        a.setDueDate(Date.valueOf("2025-09-01"));
        a.setSection(s);
        assignmentRepository.save(a);

        // change dueDate of assignment
        // dueDate must be between start and end of term for the section
        // so this PUT request should fail
        login("ted@csumb.edu", "ted2025");
        AssignmentDTO dto = new AssignmentDTO(a.getAssignmentId(), a.getTitle(), "2014-01-01", s.getCourse().getCourseId(), s.getSectionId(), s.getSectionNo());
        client.put().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void updateAssignmentBadTitle() {
        // create assignment for ted@csumb.edu for course cst334-1
        Section s = sectionRepository.findById(5).orElse(null);
        Assignment a = new Assignment();
        a.setTitle("assignment updateAssignmentBadTitle");
        a.setDueDate(Date.valueOf("2025-09-01"));
        a.setSection(s);
        assignmentRepository.save(a);

        // title cannot be blank or null, length must be <= 250,
        // only letters, digits, period, comma, space allowed
        login("ted@csumb.edu", "ted2025");

        AssignmentDTO dto = new AssignmentDTO(a.getAssignmentId(), "<p>title</p>", a.getDueDate().toString(), s.getCourse().getCourseId(), s.getSectionId(), s.getSectionNo());
        client.put().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    public void updateAssignmentNullTitle() {
        // create assignment for ted@csumb.edu for course cst334-1
        Section s = sectionRepository.findById(5).orElse(null);
        Assignment a = new Assignment();
        a.setTitle("assignment updateAssignmentNullTitle");
        a.setDueDate(Date.valueOf("2025-09-01"));
        a.setSection(s);
        assignmentRepository.save(a);

        // title cannot be blank or null, length must be <= 250,
        // only letters, digits, period, comma, space allowed
        login("ted@csumb.edu", "ted2025");

        AssignmentDTO dto = new AssignmentDTO(a.getAssignmentId(), null, a.getDueDate().toString(), s.getCourse().getCourseId(), s.getSectionId(), s.getSectionNo());
        client.put().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
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
