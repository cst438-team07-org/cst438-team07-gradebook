package com.cst438.controller;

import com.cst438.dto.GradeDTO;
import com.cst438.dto.LoginDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest                // load full application context
@AutoConfigureMockMvc         // and configure MockMvc
public class GradeControllerUnitTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Helper to log in and grab the JWT. */
    private String loginAndGetJwt(String user, String pass) throws Exception {
        MvcResult result = mvc.perform(get("/login")
                .with(SecurityMockMvcRequestPostProcessors.httpBasic(user, pass)))
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        LoginDTO login = objectMapper.readValue(body, LoginDTO.class);
        return login.jwt();


    }



    @Test
    @DisplayName("GET /assignments/1/grades → 401 if no Authorization header")
    public void getWithoutAuth_shouldReturn401() throws Exception {
        mvc.perform(get("/assignments/1/grades"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /assignments/1/grades → 200 + JSON array for valid instructor")
    public void getGrades_success() throws Exception {
        String jwt = loginAndGetJwt("ted@csumb.edu", "ted2025");

        mvc.perform(get("/assignments/1/grades")
                .header("Authorization", "Bearer " + jwt)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].studentEmail").exists());
    }

    @Test
    @DisplayName("GET /assignments/9999/grades → 400 when assignment not found")
    public void getGrades_notFound() throws Exception {
        String jwt = loginAndGetJwt("ted@csumb.edu", "ted2025");

        mvc.perform(get("/assignments/9999/grades")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isBadRequest())      // ← changed from isNotFound()
            .andExpect(jsonPath("$.errors[0]").value("Assignment not found"));
    }

    @Test
    @DisplayName("GET /assignments/1/grades → 403 if user is not the instructor")
    public void getGrades_forbidden() throws Exception {
        String jwt = loginAndGetJwt("sam@csumb.edu", "sam2025");

        mvc.perform(get("/assignments/1/grades")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /grades → 401 if no Authorization header")
    public void updateWithoutAuth_shouldReturn401() throws Exception {
        GradeDTO dto = new GradeDTO(1, "Alice", "alice@csumb.edu",
            "Homework", "cst438", 1, 90);

        mvc.perform(put("/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(dto))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /grades → 200 for valid payload and instructor")
    public void updateGrades_success() throws Exception {
        String jwt = loginAndGetJwt("ted@csumb.edu", "ted2025");

        GradeDTO good = new GradeDTO(1, "Bob", "bob@csumb.edu",
            "Lab", "cst438", 1, 85);

        mvc.perform(put("/grades")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(good))))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /grades → 400 when gradeId does not exist")
    public void updateGrades_notFound() throws Exception {
        String jwt = loginAndGetJwt("ted@csumb.edu", "ted2025");

        GradeDTO bad = new GradeDTO(9999, "Nobody", "no@csumb.edu",
            "Quiz", "cst438", 1, 50);

        mvc.perform(put("/grades")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(bad))))
            .andExpect(status().isBadRequest())      // ← changed from isNotFound()
            .andExpect(jsonPath("$.errors[0]").value("Grade not found"));
    }

    @Test
    @DisplayName("PUT /grades → 403 if user not instructor for these grades")
    public void updateGrades_forbidden() throws Exception {
        String jwt = loginAndGetJwt("sam@csumb.edu", "sam2025");

        GradeDTO dto = new GradeDTO(1, "Carl", "carl@csumb.edu",
            "Exam", "cst438", 1, 75);

        mvc.perform(put("/grades")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(dto))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /grades → 400 for validation errors (missing or out-of-range fields)")
    public void updateGrades_validationError() throws Exception {
        String jwt = loginAndGetJwt("ted@csumb.edu", "ted2025");

        // Missing 'score' and invalid email
        Map<String,Object> invalid = Map.of(
            "gradeId",         1,
            "studentName",     "X",
            "studentEmail",    "not-an-email",
            "assignmentTitle", "T",
            "courseId",        "cst438",
            "sectionId",       1
        );

        mvc.perform(put("/grades")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(invalid))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray());
    }
}
