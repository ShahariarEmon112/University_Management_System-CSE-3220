package com.kuet.cse.ums.service;

import com.kuet.cse.ums.dto.CourseDto;
import com.kuet.cse.ums.dto.RegisterRequestDto;
import com.kuet.cse.ums.dto.ResultDto;
import com.kuet.cse.ums.dto.UserResponseDto;
import com.kuet.cse.ums.entity.Role;
import com.kuet.cse.ums.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UniversityServicesTest {

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private ResultService resultService;

    @Test
    @DisplayName("Should successfully register a new student user and entity")
    void testRegisterStudentUser() {
        RegisterRequestDto req = new RegisterRequestDto();
        req.setUsername("new_student");
        req.setPassword("pass12345");
        req.setFullName("New Student Profile");
        req.setEmail("new_student@kuet.ac.bd");
        req.setRole(Role.ROLE_STUDENT);
        req.setStudentRoll("2007099");
        req.setDepartment("Computer Science & Engineering");
        req.setSeries(2022);

        UserResponseDto created = userService.register(req);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("new_student", created.getUsername());
        assertEquals(Role.ROLE_STUDENT, created.getRole());
    }

    @Test
    @DisplayName("Should throw BadRequestException if duplicate username is registered")
    void testDuplicateUsernameFails() {
        RegisterRequestDto req = new RegisterRequestDto();
        req.setUsername("admin"); // Already created in DataInitializer
        req.setPassword("pass123");
        req.setFullName("Duplicate Admin");
        req.setEmail("dup@kuet.ac.bd");
        req.setRole(Role.ROLE_ADMIN);

        assertThrows(BadRequestException.class, () -> userService.register(req));
    }

    @Test
    @DisplayName("Should create course and retrieve course list")
    void testCreateAndGetCourses() {
        CourseDto courseDto = new CourseDto();
        courseDto.setCourseCode("CSE 3201");
        courseDto.setCourseTitle("Numerical Methods");
        courseDto.setCredits(3.0);
        courseDto.setDescription("Numerical algorithm implementations");

        CourseDto created = courseService.createCourse(courseDto);
        assertNotNull(created.getId());
        assertEquals("CSE 3201", created.getCourseCode());

        List<CourseDto> allCourses = courseService.getAllCourses();
        assertTrue(allCourses.stream().anyMatch(c -> c.getCourseCode().equals("CSE 3201")));
    }

    @Test
    @DisplayName("Should calculate correct letter grade and grade point when grading")
    void testGradingComputation() {
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", "admin123", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        ResultDto resultDto = new ResultDto();
        resultDto.setEnrollmentId(1L); // Created in DataInitializer
        resultDto.setMarks(88.0);
        resultDto.setRemarks("Excellent work");

        ResultDto saved = resultService.saveOrUpdateResult(resultDto, adminAuth);
        assertNotNull(saved);
        assertEquals("A+", saved.getLetterGrade());
        assertEquals(4.0, saved.getGradePoint());
    }
}
