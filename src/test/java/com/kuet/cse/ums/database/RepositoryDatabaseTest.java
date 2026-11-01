package com.kuet.cse.ums.database;

import com.kuet.cse.ums.entity.*;
import com.kuet.cse.ums.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database tests: only the JPA layer is loaded and real SQL runs against a real database.
 * Locally this is in-memory H2 (MySQL mode); in GitHub Actions the CI workflow points
 * SPRING_DATASOURCE_URL at a MySQL 8 service container, the same engine used in production.
 * The schema is created from the entities, and every test is rolled back afterwards.
 */
@Tag("database")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryDatabaseTest {

    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private ResultRepository resultRepository;
    @Autowired private TestEntityManager entityManager;

    private Student student;
    private Course course;

    @BeforeEach
    void seed() {
        User teacherUser = userRepository.save(new User("t_db", "hashed-pw", "DB Teacher", "t_db@kuet.ac.bd", Role.ROLE_TEACHER));
        Teacher teacher = teacherRepository.save(new Teacher(teacherUser, "CSE", "Lecturer", null));
        User studentUser = userRepository.save(new User("s_db", "hashed-pw", "DB Student", "s_db@kuet.ac.bd", Role.ROLE_STUDENT));
        student = studentRepository.save(new Student(studentUser, "DB001", "CSE", 2021, null));
        course = courseRepository.save(new Course("DB 1001", "Database Testing", 3.0, null, teacher));
        userRepository.flush();
    }

    @Test
    @DisplayName("D1: A saved user can be read back by username and the role is stored as text")
    void saveAndFindUser() {
        User found = userRepository.findByUsername("s_db").orElseThrow();
        assertNotNull(found.getId());
        assertEquals(Role.ROLE_STUDENT, found.getRole());
        assertTrue(userRepository.existsByEmail("s_db@kuet.ac.bd"));
        assertTrue(userRepository.findByUsername("nobody").isEmpty());
    }

    @Test
    @DisplayName("D2: The database rejects a duplicate username (UNIQUE constraint)")
    void uniqueUsernameEnforced() {
        User dup = new User("s_db", "hashed-pw", "Someone Else", "other@kuet.ac.bd", Role.ROLE_STUDENT);
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(dup));
    }

    @Test
    @DisplayName("D3: The database rejects the same student/course/semester enrollment twice")
    void uniqueEnrollmentEnforced() {
        enrollmentRepository.saveAndFlush(new Enrollment(student, course, "Fall 2026"));
        assertThrows(DataIntegrityViolationException.class,
                () -> enrollmentRepository.saveAndFlush(new Enrollment(student, course, "Fall 2026")));
    }

    @Test
    @DisplayName("D4: Results can be queried by student and by course through the enrollment join")
    void resultQueriesAcrossJoins() {
        Enrollment e = enrollmentRepository.save(new Enrollment(student, course, "Fall 2026"));
        resultRepository.saveAndFlush(new Result(e, 76.0, "Good"));

        List<Result> byStudent = resultRepository.findByEnrollmentStudentId(student.getId());
        List<Result> byCourse = resultRepository.findByEnrollmentCourseId(course.getId());

        assertEquals(1, byStudent.size());
        assertEquals(1, byCourse.size());
        assertEquals("A", byStudent.get(0).getLetterGrade());      // grade stored with the row
        assertEquals(3.75, byCourse.get(0).getGradePoint());
        assertTrue(resultRepository.existsByEnrollmentId(e.getId()));
    }

    @Test
    @DisplayName("D5: A graded enrollment cannot be deleted directly (FOREIGN KEY), so services must delete the result first")
    void foreignKeyBlocksDeletingGradedEnrollment() {
        Enrollment e = enrollmentRepository.save(new Enrollment(student, course, "Fall 2026"));
        resultRepository.saveAndFlush(new Result(e, 50.0, null));
        entityManager.clear(); // forget cached objects so the DELETE really reaches the database

        enrollmentRepository.deleteById(e.getId());
        assertThrows(DataIntegrityViolationException.class, () -> enrollmentRepository.flush());
    }
}
