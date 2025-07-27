package com.cst438.controller2;

import com.cst438.domain.EnrollmentRepository;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AssignmentCreateFailUnitTest {

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
    public void getAssignmentsInvalidSectionNoFails() {
        login("ted@csumb.edu", "ted2025");
        client.get().uri("/sections/9999/assignments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();

    }

    @Test
    public void createAssignmentBadSectionFails() {
        login("ted@csumb.edu", "ted2025");
        AssignmentDTO dto = new AssignmentDTO(
                0,
                "test assignment",
                "2025-09-01",
                null,
                0,
                9999 // section does not exist
        );
        client.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().is4xxClientError();

    }

    @Test
    public void createAssignmentAnotherUserFails() {
        // login as ted@csumb.edu and attempt to create assignment for section 6 belonging to ted2@csumb.edu
        login("ted@csumb.edu", "ted2025");
        AssignmentDTO dto = new AssignmentDTO(
                0,
                "test assignment",
                "2025-09-01",
                null,
                0,
                6
        );
        client.post().uri("/assignments")
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
