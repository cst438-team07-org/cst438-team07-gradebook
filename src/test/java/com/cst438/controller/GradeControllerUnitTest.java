package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.GradeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GradeControllerUnitTest {

    @Autowired
    private WebTestClient client;

    @Test
    public void getAssignmentGrades_Success() {
        // Login as instructor (ted@csumb.edu)
        EntityExchangeResult<LoginDTO> login = client.get().uri("/login")
                .headers(h -> h.setBasicAuth("ted@csumb.edu", "ted2025"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();

        String jwt = login.getResponseBody().jwt();
        assertNotNull(jwt);

        // Use an existing assignment ID you know is assigned to ted@csumb.edu
        int assignmentId = 1;

        // Request grades for the assignment
        EntityExchangeResult<GradeDTO[]> gradesResult = client.get()
                .uri("/assignments/" + assignmentId + "/grades")
                .headers(h -> h.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GradeDTO[].class)
                .returnResult();

        GradeDTO[] grades = gradesResult.getResponseBody();
        assertNotNull(grades);
        assertTrue(grades.length > 0, "Grades list should not be empty");

        // Verify fields of the first GradeDTO
        GradeDTO firstGrade = grades[0];
        assertNotNull(firstGrade.studentEmail());
        assertNotNull(firstGrade.studentName());
        assertEquals(assignmentId, firstGrade.assignmentId());
    }

    @Test
    public void getAssignmentGrades_ForbiddenWhenNotInstructor() {
        // Login as user who is NOT the instructor (e.g., sam@csumb.edu)
        EntityExchangeResult<LoginDTO> login = client.get().uri("/login")
                .headers(h -> h.setBasicAuth("sam@csumb.edu", "sam2025"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();

        String jwt = login.getResponseBody().jwt();
        assertNotNull(jwt);

        int assignmentId = 1;

        // This user is not the instructor of this assignment, so access should be forbidden
        client.get()
                .uri("/assignments/" + assignmentId + "/grades")
                .headers(h -> h.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    public void updateGrades_Success() {
        // Login as instructor (ted@csumb.edu)
        EntityExchangeResult<LoginDTO> login = client.get().uri("/login")
                .headers(h -> h.setBasicAuth("ted@csumb.edu", "ted2025"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();

        String jwt = login.getResponseBody().jwt();
        assertNotNull(jwt);

        // Prepare a sample GradeDTO list with at least one grade you know exists
        GradeDTO gradeToUpdate = new GradeDTO(
                1,                 // gradeId - replace with existing gradeId
                "Student Name",    // studentName (not used for update, can be anything)
                "student@csumb.edu", // studentEmail
                "Assignment Title",  // assignmentTitle
                "cst363",            // courseId
                1,                   // sectionId
                95.0                 // score to update
        );

        // Send PUT request to update the grade score
        client.put().uri("/grades")
                .headers(h -> h.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(gradeToUpdate))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void updateGrades_ForbiddenWhenNotInstructor() {
        // Login as user who is NOT instructor (sam@csumb.edu)
        EntityExchangeResult<LoginDTO> login = client.get().uri("/login")
                .headers(h -> h.setBasicAuth("sam@csumb.edu", "sam2025"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();

        String jwt = login.getResponseBody().jwt();
        assertNotNull(jwt);

        // Prepare grade DTO
        GradeDTO gradeToUpdate = new GradeDTO(
                1,
                "Student Name",
                "student@csumb.edu",
                "Assignment Title",
                "cst363",
                1,
                80.0
        );

        // This user should NOT be authorized to update the grade
        client.put().uri("/grades")
                .headers(h -> h.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(gradeToUpdate))
                .exchange()
                .expectStatus().isForbidden();
    }
}
