package com.kuet.cse.ums.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuet.cse.ums.repository.EnrollmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests: the full application starts (security filters, controllers, services,
 * JPA and the seeded demo data) and each test drives it through real HTTP requests,
 * logging in with a session cookie exactly like the web portal does.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // each test's changes are rolled back
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private MockHttpSession login(String username, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession();
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    @DisplayName("I1: Login creates a session that identifies the user; logout ends it")
    void loginSessionAndLogout() throws Exception {
        MockHttpSession session = login("student_sakib", "student123");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("student_sakib"))
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("I2: Admin creates a course, enrolls a student, and the roster shows the student")
    void adminCreatesCourseAndEnrollsStudent() throws Exception {
        MockHttpSession admin = login("admin", "admin123");

        String courseBody = mockMvc.perform(post("/api/courses").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("courseCode", "CSE 4100", "courseTitle", "Machine Learning", "credits", 3.0))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long courseId = objectMapper.readTree(courseBody).get("id").asLong();

        String students = mockMvc.perform(get("/api/students").session(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long sakibId = -1;
        for (JsonNode s : objectMapper.readTree(students)) {
            if ("2007001".equals(s.get("studentRoll").asText())) sakibId = s.get("id").asLong();
        }

        mockMvc.perform(post("/api/enrollments").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("studentId", sakibId, "courseId", courseId, "semester", "Spring 2027"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/enrollments/course/" + courseId).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].studentRoll", hasItem("2007001")))
                .andExpect(jsonPath("$[0].courseCode").value("CSE 4100"));

        // A student must not be able to do the same admin action
        MockHttpSession student = login("student_tamim", "student123");
        mockMvc.perform(post("/api/courses").session(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("courseCode", "CSE 4999", "courseTitle", "Hack", "credits", 3.0))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I3: Teacher grades an enrolled student and the student sees the grade")
    void teacherGradesAndStudentSeesResult() throws Exception {
        // Sakib is enrolled in CSE 3200 (taught by dr_karim) and has a seeded grade there
        long enrollmentId = enrollmentRepository.findAll().stream()
                .filter(e -> e.getCourse().getCourseCode().equals("CSE 3200")
                        && e.getStudent().getStudentRoll().equals("2007001"))
                .findFirst().orElseThrow().getId();

        MockHttpSession teacher = login("dr_karim", "teacher123");
        mockMvc.perform(post("/api/results").session(teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enrollmentId", enrollmentId, "marks", 72.0, "remarks", "Good project"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.letterGrade").value("A-"))
                .andExpect(jsonPath("$.gradePoint").value(3.5));

        MockHttpSession student = login("student_sakib", "student123");
        mockMvc.perform(get("/api/results/my-results").session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.courseCode == 'CSE 3200')].letterGrade", hasItem("A-")))
                .andExpect(jsonPath("$[?(@.courseCode == 'CSE 3200')].remarks", hasItem("Good project")));
    }
}
