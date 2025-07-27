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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AssignmentCreateUpdateDeleteOkUnitTest {

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
    public void createAssignmentUpdateDeleteOk() {
        login("ted@csumb.edu", "ted2025");
        Section s = sectionRepository.findById(5).orElse(null);
        AssignmentDTO dto = new AssignmentDTO(0, "assignment test", "2025-09-01", s.getCourse().getCourseId(), s.getSectionId(), s.getSectionNo());
        EntityExchangeResult<AssignmentDTO>  result = client.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class)
                .returnResult();
        Assignment a = assignmentRepository.findById(result.getResponseBody().id()).orElse(null);
        // verify the assignment entity from the database
        assertEquals("assignment test", a.getTitle());
        assertEquals("2025-09-01", a.getDueDate().toString());
        assertEquals(5, a.getSection().getSectionNo());

        // list the assignments and verify that new assignment is in list.
        EntityExchangeResult<AssignmentDTO[]>  aList = client.get().uri(String.format("/sections/%s/assignments", s.getSectionNo()))
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO[].class)
                .returnResult();

        boolean found=false;
        for (AssignmentDTO d : aList.getResponseBody()) {
            if (d.id()==a.getAssignmentId()) {
                found=true;
                break;
            }
        }
        assertTrue(found);

        //update title
        dto = new AssignmentDTO(a.getAssignmentId(), "assignment title changed", "2025-09-02", s.getCourse().getCourseId(), s.getSectionId(), s.getSectionNo());
        result = client.put().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class)
                .returnResult();
        // verify that title and dueDate are changed in returned result
        assertEquals("assignment title changed", result.getResponseBody().title());
        assertEquals("2025-09-02", result.getResponseBody().dueDate().toString());
        // verify that title and dueDate are changed in database
        a = assignmentRepository.findById(result.getResponseBody().id()).orElse(null);
        assertEquals("assignment title changed", a.getTitle());
        assertEquals("2025-09-02", a.getDueDate().toString());


        // delete the assignment
        client.delete().uri("/assignments/"+a.getAssignmentId())
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .exchange()
                .expectStatus().isOk();
        // verify delete in database
        a = assignmentRepository.findById(a.getAssignmentId()).orElse(null);
        assertNull(a);
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
