package com.kuet.cse.ums.service;

import com.kuet.cse.ums.dto.CourseDto;
import com.kuet.cse.ums.dto.RegisterRequestDto;
import com.kuet.cse.ums.dto.ResultDto;
import com.kuet.cse.ums.dto.UserResponseDto;
import com.kuet.cse.ums.entity.Role;
import com.kuet.cse.ums.exception.BadRequestException;
import com.kuet.cse.ums.exception.UnauthorizedException;
import com.kuet.cse.ums.repository.CourseRepository;
import com.kuet.cse.ums.repository.EnrollmentRepository;
import com.kuet.cse.ums.repository.ResultRepository;
import com.kuet.cse.ums.repository.StudentRepository;
import com.kuet.cse.ums.repository.TeacherRepository;
import com.kuet.cse.ums.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
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

@Tag("regression")
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

    @Autowired
    private StudentService studentService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ResultRepository resultRepository;

    private static Authentication auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username, "n/a", Collections.singletonList(new SimpleGrantedAuthority(role)));
    }

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

    @Test
    @DisplayName("Deleting a course that has enrollments and results also removes them")
    void testDeleteCourseWithEnrollmentsAndResults() {
        Long courseId = courseRepository.findByCourseCode("CSE 3220").orElseThrow().getId();
        assertFalse(enrollmentRepository.findByCourseId(courseId).isEmpty());
        assertFalse(resultRepository.findByEnrollmentCourseId(courseId).isEmpty());

        courseService.deleteCourse(courseId);
        courseRepository.flush();

        assertFalse(courseRepository.existsById(courseId));
        assertTrue(enrollmentRepository.findByCourseId(courseId).isEmpty());
        assertTrue(resultRepository.findByEnrollmentCourseId(courseId).isEmpty());
    }

    @Test
    @DisplayName("Deleting a graded enrollment also removes its result")
    void testDeleteGradedEnrollment() {
        Long enrollmentId = resultRepository.findAll().get(0).getEnrollment().getId();

        enrollmentService.deleteEnrollment(enrollmentId);
        enrollmentRepository.flush();

        assertFalse(enrollmentRepository.existsById(enrollmentId));
        assertFalse(resultRepository.existsByEnrollmentId(enrollmentId));
    }

    @Test
    @DisplayName("Deleting a student removes their enrollments, results and login account")
    void testDeleteStudentCascades() {
        var student = studentRepository.findByUserUsername("student_sakib").orElseThrow();
        Long studentId = student.getId();
        Long userId = student.getUser().getId();

        studentService.deleteStudent(studentId);
        studentRepository.flush();

        assertFalse(studentRepository.existsById(studentId));
        assertFalse(userRepository.existsById(userId));
        assertTrue(enrollmentRepository.findByStudentId(studentId).isEmpty());
        assertTrue(resultRepository.findByEnrollmentStudentId(studentId).isEmpty());
    }

    @Test
    @DisplayName("Deleting a teacher keeps their courses but leaves them unassigned")
    void testDeleteTeacherUnassignsCourses() {
        var teacher = teacherRepository.findByUserUsername("dr_karim").orElseThrow();
        Long teacherId = teacher.getId();
        assertFalse(courseRepository.findByTeacherId(teacherId).isEmpty());

        teacherService.deleteTeacher(teacherId);
        teacherRepository.flush();

        assertFalse(teacherRepository.existsById(teacherId));
        assertFalse(userRepository.existsByUsername("dr_karim"));
        assertTrue(courseRepository.findByTeacherId(teacherId).isEmpty());
        assertTrue(courseRepository.findByCourseCode("CSE 3220").isPresent());
    }

    @Test
    @DisplayName("Deleting a user with a student profile works through the user endpoint")
    void testDeleteUserWithStudentProfile() {
        Long userId = userRepository.findByUsername("student_tamim").orElseThrow().getId();

        userService.deleteUser(userId, auth("admin", "ROLE_ADMIN"));
        userRepository.flush();

        assertFalse(userRepository.existsById(userId));
        assertTrue(studentRepository.findByUserUsername("student_tamim").isEmpty());
    }

    @Test
    @DisplayName("An admin cannot delete their own account")
    void testAdminCannotDeleteSelf() {
        Long adminId = userRepository.findByUsername("admin").orElseThrow().getId();
        assertThrows(BadRequestException.class, () -> userService.deleteUser(adminId, auth("admin", "ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Anonymous registration cannot create an ADMIN account")
    void testPublicRegisterCannotCreateAdmin() {
        RegisterRequestDto req = new RegisterRequestDto();
        req.setUsername("sneaky");
        req.setPassword("pass12345");
        req.setFullName("Sneaky User");
        req.setEmail("sneaky@kuet.ac.bd");
        req.setRole(Role.ROLE_ADMIN);

        assertThrows(UnauthorizedException.class, () -> userService.register(req, null));
        assertFalse(userRepository.existsByUsername("sneaky"));
    }

    @Test
    @DisplayName("A teacher cannot view results of a course they do not teach")
    void testTeacherCannotViewOtherCourseResults() {
        Long osCourseId = courseRepository.findByCourseCode("CSE 3211").orElseThrow().getId(); // taught by prof_rahim
        assertThrows(UnauthorizedException.class,
                () -> resultService.getResultsByCourseId(osCourseId, auth("dr_karim", "ROLE_TEACHER")));
    }
}
