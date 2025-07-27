package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Date;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AssignmentControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SectionRepository sectionRepository;

    @MockBean
    private AssignmentRepository assignmentRepository;

    @MockBean
    private GradeRepository gradeRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "instructor@test.edu", authorities = {"SCOPE_ROLE_INSTRUCTOR"})
    public void testCreateAssignment_Success() throws Exception {

        // Mock Section
        Term term = new Term();
        term.setStartDate(Date.valueOf("2025-01-01"));
        term.setEndDate(Date.valueOf("2025-05-01"));

        Course course = new Course();
        course.setCourseId("CST438");
        course.setTitle("Software Engineering");

        Section section = new Section();
        section.setSectionNo(101);
        section.setSectionId(1001);
        section.setInstructorEmail("instructor@test.edu");
        section.setCourse(course);
        section.setTerm(term);

        given(sectionRepository.findById(1001)).willReturn(Optional.of(section));

        // Mock saved assignment
        Assignment savedAssignment = new Assignment();
        savedAssignment.setAssignmentId(100);
        savedAssignment.setTitle("HW1");
        savedAssignment.setDueDate(Date.valueOf("2025-03-01"));
        savedAssignment.setSection(section);

        given(assignmentRepository.save(ArgumentMatchers.any())).willReturn(savedAssignment);

        AssignmentDTO dto = new AssignmentDTO(
                0,
                "HW1",
                "2025-03-01",
                "CST438",
                1001,
                101
        );

        mockMvc.perform(post("/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("HW1"))
                .andExpect(jsonPath("$.dueDate").value("2025-03-01"))
                .andExpect(jsonPath("$.courseId").value("CST438"))
                .andExpect(jsonPath("$.secNo").value(101));
    }
}
