package com.cst438.controller2;

import com.cst438.domain.*;
import com.cst438.dto.GradeDTO;
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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AssignmentGradeUnitTest {

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
    @Autowired
    GradeRepository gradeRepository;


    Random random = new Random();


    // get assignment grades /assignments/{id}/grades
    // success
    // login as ted@csumb.edu
    // create assignment for section #1
    @Test
    public void getAssignmentGradesFail() {

        login("ted@csumb.edu", "ted2025");

        // get grades for assignment that does not exist
        client.get().uri("/assignments/9999/grades")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();

        // create assignment for ted2@csumb.edu section #6
        Section s = sectionRepository.findById(6).orElse(null);
        Assignment a = new Assignment();
        a.setSection(s);
        a.setTitle("Test");
        a.setDueDate(Date.valueOf("2025-09-01"));
        assignmentRepository.save(a);

        // ted@csumb.edu attempts to get grades for the assignment which should fail
        client.get().uri(String.format("/assignments/%s/grades",a.getAssignmentId()))
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();

    }

    @Test
    public void getAndPutAssignmentGradesOk() {
        login("ted@csumb.edu", "ted2025");
        // create assignment for ted@csumb.edu section #1
        Section s = sectionRepository.findById(1).orElse(null);
        Assignment a = new Assignment();
        a.setSection(s);
        a.setTitle("Test");
        a.setDueDate(Date.valueOf("2025-09-01"));
        assignmentRepository.save(a);

        // get grades for assignment
        GradeDTO[] dtolist = client.get().uri(String.format("/assignments/%s/grades",a.getAssignmentId()))
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GradeDTO[].class).returnResult().getResponseBody();

        // there are 2 students enrolled, so there should be two grades
        assertEquals(2, dtolist.length, "incorrect number of grades returned");

        // change the scores
        GradeDTO[] scores = Arrays.stream(dtolist).map(
                gradeDTO -> new GradeDTO(
                        gradeDTO.gradeId(),
                        gradeDTO.studentName(),
                        gradeDTO.studentEmail(),
                        gradeDTO.assignmentTitle(),
                        gradeDTO.courseId(),
                        gradeDTO.sectionId(),
                        random.nextInt(100)  // random score
                )
        ).toArray(GradeDTO[]::new);

        client.put().uri("/grades")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(scores)
                .exchange()
                .expectStatus().isOk();
        // verify scores in database
        for (GradeDTO gradeDTO: scores) {
            Grade g = gradeRepository.findById(gradeDTO.gradeId()).orElse(null);
            assertEquals(gradeDTO.score(), g.getScore(), "score value in database incorrect");
        }

        // fetch the grades again and compare the list of scores sent to server with scores returned
        dtolist = client.get().uri(String.format("/assignments/%s/grades",a.getAssignmentId()))
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GradeDTO[].class).returnResult().getResponseBody();

        assertEquals(2, dtolist.length, "incorrect number of grades returned");

        // we assume that the lists are in the same order.  This may not be true.
       for (int i=0; i<2; i++) {
           assertEquals(scores[i].gradeId(), dtolist[i].gradeId(), "lists are not in same order");
           assertEquals(scores[i].studentEmail(), dtolist[i].studentEmail(), "student emails don't match");
           assertEquals(scores[i].score(), dtolist[i].score(), "grade scores returned are not the scores sent");
       }
    }

    // put grades
    //* grade id does not exist
    //* grade id does not belong to instructor

    @Test
    public void updateAssignmentScoreFails() {
        // create assignment for ted@csumb.edu section #2
        Section s = sectionRepository.findById(2).orElse(null);
        Assignment a = new Assignment();
        a.setSection(s);
        a.setTitle("updateAssignmentScoreFails");
        a.setDueDate(Date.valueOf("2025-09-01"));
        assignmentRepository.save(a);

        //login as ted@csumb.edu and get assignment grades
        login("ted@csumb.edu", "ted2025");

        GradeDTO[] dtolist = client.get().uri(String.format("/assignments/%s/grades",a.getAssignmentId()))
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GradeDTO[].class).returnResult().getResponseBody();
        assertEquals(4, dtolist.length, "number of returned grades is expected to be 4");

        // login as ted@csumb.edu and attempt to update a grade that does not exist
        login("ted2@csumb.edu", "ted2025");

        GradeDTO[] scores = new GradeDTO[] {
                new GradeDTO(9999,
                        "sam",
                        "sam@csumb.edu",
                        "assignment title",
                        "cst489",
                        1,
                        random.nextInt(100)  // random score
                )
        };

        //  attempt to update a grade that does not exist, invalid grade id.
        client.put().uri("/grades")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(scores)
                .exchange()
                .expectStatus().is4xxClientError();


        // attempt to update grades that belong to ted2@csumb.edu
        client.put().uri("/grades")
                .headers(headers -> headers.setBearerAuth(loginJWT))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dtolist)
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
