package com.kuet.cse.ums.unit;

import com.kuet.cse.ums.dto.CourseDto;
import com.kuet.cse.ums.dto.EnrollmentDto;
import com.kuet.cse.ums.dto.RegisterRequestDto;
import com.kuet.cse.ums.dto.ResultDto;
import com.kuet.cse.ums.entity.*;
import com.kuet.cse.ums.exception.BadRequestException;
import com.kuet.cse.ums.exception.UnauthorizedException;
import com.kuet.cse.ums.repository.*;
import com.kuet.cse.ums.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests: each test checks one class in isolation.
 * Repositories and other collaborators are Mockito mocks, so no Spring context or database is used.
 */
@Tag("unit")
class ServiceUnitTest {

    private static Authentication auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(username, "n/a", List.of(new SimpleGrantedAuthority(role)));
    }

    // ---------------------------------------------------------------- 1
    @Test
    @DisplayName("U1: Marks are converted to the correct KUET letter grade at each boundary")
    void gradeBoundaries() {
        Result r = new Result();

        r.setMarksAndComputeGrade(80.0);
        assertEquals("A+", r.getLetterGrade());
        assertEquals(4.00, r.getGradePoint());

        r.setMarksAndComputeGrade(79.5);
        assertEquals("A", r.getLetterGrade());
        assertEquals(3.75, r.getGradePoint());

        r.setMarksAndComputeGrade(40.0);
        assertEquals("D", r.getLetterGrade());

        r.setMarksAndComputeGrade(39.5);
        assertEquals("F", r.getLetterGrade());
        assertEquals(0.0, r.getGradePoint());
    }

    // ---------------------------------------------------------------- 2
    @Test
    @DisplayName("U2: Anonymous sign-up cannot create an ADMIN account")
    void publicRegistrationRejectsAdminRole() {
        UserRepository userRepo = mock(UserRepository.class);
        UserService service = new UserService(userRepo, mock(StudentRepository.class), mock(TeacherRepository.class),
                mock(PasswordEncoder.class), mock(AuthenticationManager.class),
                mock(StudentService.class), mock(TeacherService.class));

        RegisterRequestDto req = new RegisterRequestDto();
        req.setUsername("intruder");
        req.setPassword("secret123");
        req.setFullName("Intruder");
        req.setEmail("intruder@example.com");
        req.setRole(Role.ROLE_ADMIN);

        assertThrows(UnauthorizedException.class, () -> service.register(req, null));
        verify(userRepo, never()).save(any());
    }

    // ---------------------------------------------------------------- 3
    @Test
    @DisplayName("U3: Registering a student hashes the password and creates a student profile")
    void registerStudentHashesPasswordAndCreatesProfile() {
        UserRepository userRepo = mock(UserRepository.class);
        StudentRepository studentRepo = mock(StudentRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserService service = new UserService(userRepo, studentRepo, mock(TeacherRepository.class),
                encoder, mock(AuthenticationManager.class),
                mock(StudentService.class), mock(TeacherService.class));

        when(encoder.encode("plainPass1")).thenReturn("HASHED");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        RegisterRequestDto req = new RegisterRequestDto();
        req.setUsername("  new_student ");
        req.setPassword("plainPass1");
        req.setFullName("New Student");
        req.setEmail("New@Stud.KUET.ac.bd");
        req.setRole(Role.ROLE_STUDENT);
        req.setStudentRoll("2107001");
        req.setSeries(2021);

        var created = service.register(req, null);

        assertEquals("new_student", created.getUsername());          // trimmed
        assertEquals("new@stud.kuet.ac.bd", created.getEmail());     // lower-cased
        verify(userRepo).save(argThat(u -> u.getPassword().equals("HASHED")));
        verify(studentRepo).save(argThat(s -> s.getStudentRoll().equals("2107001") && s.getSeries() == 2021));
    }

    // ---------------------------------------------------------------- 4
    @Test
    @DisplayName("U4: Creating a course with an existing code (any casing) is rejected")
    void duplicateCourseCodeRejected() {
        CourseRepository courseRepo = mock(CourseRepository.class);
        CourseService service = new CourseService(courseRepo, mock(TeacherRepository.class),
                mock(EnrollmentRepository.class), mock(ResultRepository.class));
        when(courseRepo.existsByCourseCode("CSE 3220")).thenReturn(true);

        CourseDto dto = new CourseDto();
        dto.setCourseCode(" cse 3220 ");
        dto.setCourseTitle("Duplicate");
        dto.setCredits(1.5);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.createCourse(dto));
        assertTrue(ex.getMessage().contains("CSE 3220"));
        verify(courseRepo, never()).save(any());
    }

    // ---------------------------------------------------------------- 5
    @Test
    @DisplayName("U5: Enrolling twice in the same semester is rejected even if the semester is typed differently")
    void duplicateEnrollmentWithDifferentSemesterCasingRejected() {
        EnrollmentRepository enrollmentRepo = mock(EnrollmentRepository.class);
        StudentRepository studentRepo = mock(StudentRepository.class);
        CourseRepository courseRepo = mock(CourseRepository.class);
        EnrollmentService service = new EnrollmentService(enrollmentRepo, studentRepo, courseRepo, mock(ResultRepository.class));

        Student student = new Student();
        student.setId(1L);
        Course course = new Course();
        course.setId(7L);
        course.setCourseCode("CSE 3220");
        when(studentRepo.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepo.findById(7L)).thenReturn(Optional.of(course));
        // The student is already enrolled for "Fall 2026"
        when(enrollmentRepo.existsByStudentIdAndCourseIdAndSemester(1L, 7L, "Fall 2026")).thenReturn(true);

        EnrollmentDto dto = new EnrollmentDto();
        dto.setStudentId(1L);
        dto.setCourseId(7L);
        dto.setSemester("  fall   2026 ");

        assertThrows(BadRequestException.class, () -> service.enrollStudent(dto));
        verify(enrollmentRepo, never()).save(any());
    }

    // ---------------------------------------------------------------- 6
    @Test
    @DisplayName("U6: A teacher cannot grade a course assigned to another teacher")
    void teacherCannotGradeOtherTeachersCourse() {
        EnrollmentRepository enrollmentRepo = mock(EnrollmentRepository.class);
        ResultRepository resultRepo = mock(ResultRepository.class);
        ResultService service = new ResultService(resultRepo, enrollmentRepo,
                mock(StudentRepository.class), mock(CourseRepository.class));

        User owner = new User("prof_rahim", "x", "Prof. Rahim", "r@kuet.ac.bd", Role.ROLE_TEACHER);
        Course course = new Course("CSE 3211", "Operating Systems", 3.0, null, new Teacher(owner, "CSE", "Professor", null));
        Enrollment enrollment = new Enrollment(new Student(), course, "Fall 2026");
        enrollment.setId(5L);
        when(enrollmentRepo.findById(5L)).thenReturn(Optional.of(enrollment));

        ResultDto dto = new ResultDto();
        dto.setEnrollmentId(5L);
        dto.setMarks(90.0);

        assertThrows(UnauthorizedException.class, () -> service.saveOrUpdateResult(dto, auth("dr_karim", "ROLE_TEACHER")));
        verify(resultRepo, never()).save(any());
    }
}
