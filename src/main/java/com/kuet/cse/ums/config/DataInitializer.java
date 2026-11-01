package com.kuet.cse.ums.config;

import com.kuet.cse.ums.entity.*;
import com.kuet.cse.ums.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            ResultRepository resultRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // Safe repeat check: only initialize if admin doesn't exist
            if (userRepository.existsByUsername("admin")) {
                log.info("Database already initialized with admin user. Skipping sample data population.");
                return;
            }

            log.info("Initializing database with demo accounts and sample data...");

            // 1. Initial Admin Account (BCrypt hashed)
            User admin = new User(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "System Administrator",
                    "admin@kuet.ac.bd",
                    Role.ROLE_ADMIN
            );
            userRepository.save(admin);

            // 2. Demo Teacher Accounts
            User teacherUser1 = new User(
                    "dr_karim",
                    passwordEncoder.encode("teacher123"),
                    "Dr. Md. Abdul Karim",
                    "karim@cse.kuet.ac.bd",
                    Role.ROLE_TEACHER
            );
            userRepository.save(teacherUser1);
            Teacher teacher1 = new Teacher(
                    teacherUser1,
                    "Computer Science & Engineering",
                    "Professor",
                    "+8801711223344"
            );
            teacherRepository.save(teacher1);

            User teacherUser2 = new User(
                    "prof_rahim",
                    passwordEncoder.encode("teacher123"),
                    "Prof. Rahim Ahmed",
                    "rahim@cse.kuet.ac.bd",
                    Role.ROLE_TEACHER
            );
            userRepository.save(teacherUser2);
            Teacher teacher2 = new Teacher(
                    teacherUser2,
                    "Computer Science & Engineering",
                    "Associate Professor",
                    "+8801811223344"
            );
            teacherRepository.save(teacher2);

            User teacherUser3 = new User(
                    "dr_nusrat",
                    passwordEncoder.encode("teacher123"),
                    "Dr. Nusrat Jahan",
                    "nusrat@cse.kuet.ac.bd",
                    Role.ROLE_TEACHER
            );
            userRepository.save(teacherUser3);
            Teacher teacher3 = new Teacher(
                    teacherUser3,
                    "Computer Science & Engineering",
                    "Assistant Professor",
                    "+8801711889900"
            );
            teacherRepository.save(teacher3);

            // 3. Demo Student Accounts
            User studentUser1 = new User(
                    "student_sakib",
                    passwordEncoder.encode("student123"),
                    "Sakib Al Hasan",
                    "sakib@stud.kuet.ac.bd",
                    Role.ROLE_STUDENT
            );
            userRepository.save(studentUser1);
            Student student1 = new Student(
                    studentUser1,
                    "2007001",
                    "Computer Science & Engineering",
                    2020,
                    "+8801911223344"
            );
            studentRepository.save(student1);

            User studentUser2 = new User(
                    "student_tamim",
                    passwordEncoder.encode("student123"),
                    "Tamim Iqbal",
                    "tamim@stud.kuet.ac.bd",
                    Role.ROLE_STUDENT
            );
            userRepository.save(studentUser2);
            Student student2 = new Student(
                    studentUser2,
                    "2007002",
                    "Computer Science & Engineering",
                    2020,
                    "+8801955667788"
            );
            studentRepository.save(student2);

            User studentUser3 = new User(
                    "student_mushfiq",
                    passwordEncoder.encode("student123"),
                    "Mushfiqur Rahim",
                    "mushfiq@stud.kuet.ac.bd",
                    Role.ROLE_STUDENT
            );
            userRepository.save(studentUser3);
            Student student3 = new Student(
                    studentUser3,
                    "2007003",
                    "Computer Science & Engineering",
                    2021,
                    "+8801722334455"
            );
            studentRepository.save(student3);

            User studentUser4 = new User(
                    "student_mahmudullah",
                    passwordEncoder.encode("student123"),
                    "Mahmudullah Riyad",
                    "mahmudullah@stud.kuet.ac.bd",
                    Role.ROLE_STUDENT
            );
            userRepository.save(studentUser4);
            Student student4 = new Student(
                    studentUser4,
                    "2007004",
                    "Computer Science & Engineering",
                    2021,
                    "+8801733445566"
            );
            studentRepository.save(student4);

            // 4. Sample Courses
            Course cse3220 = new Course(
                    "CSE 3220",
                    "Software Development Lab III",
                    1.5,
                    "Enterprise application development with Spring Boot and modern architectures.",
                    teacher1
            );
            courseRepository.save(cse3220);

            Course cse3200 = new Course(
                    "CSE 3200",
                    "Database Systems",
                    3.0,
                    "Relational algebra, SQL queries, transaction processing, and normalization.",
                    teacher1
            );
            courseRepository.save(cse3200);

            Course cse3211 = new Course(
                    "CSE 3211",
                    "Operating Systems",
                    3.0,
                    "Process scheduling, threads, memory management, and file systems.",
                    teacher2
            );
            courseRepository.save(cse3211);

            Course cse3205 = new Course(
                    "CSE 3205",
                    "Computer Networks",
                    3.0,
                    "OSI & TCP/IP models, routing protocols, TCP congestion control, socket programming.",
                    teacher3
            );
            courseRepository.save(cse3205);

            // 5. Sample Enrollments
            Enrollment e1 = new Enrollment(student1, cse3220, "Fall 2026");
            enrollmentRepository.save(e1);

            Enrollment e2 = new Enrollment(student1, cse3200, "Fall 2026");
            enrollmentRepository.save(e2);

            Enrollment e3 = new Enrollment(student2, cse3220, "Fall 2026");
            enrollmentRepository.save(e3);

            Enrollment e4 = new Enrollment(student2, cse3211, "Fall 2026");
            enrollmentRepository.save(e4);

            Enrollment e5 = new Enrollment(student3, cse3205, "Fall 2026");
            enrollmentRepository.save(e5);

            Enrollment e6 = new Enrollment(student3, cse3220, "Fall 2026");
            enrollmentRepository.save(e6);

            Enrollment e7 = new Enrollment(student4, cse3211, "Fall 2026");
            enrollmentRepository.save(e7);

            Enrollment e8 = new Enrollment(student4, cse3205, "Fall 2026");
            enrollmentRepository.save(e8);

            // 6. Sample Grades / Results
            Result r1 = new Result(e1, 85.5, "Outstanding performance in term project.");
            resultRepository.save(r1);

            Result r2 = new Result(e2, 78.0, "Good performance in mid-term and lab tasks.");
            resultRepository.save(r2);

            Result r3 = new Result(e3, 91.0, "Exceptional coding quality and test coverage.");
            resultRepository.save(r3);

            Result r4 = new Result(e4, 72.5, "Solid understanding of synchronization primitives.");
            resultRepository.save(r4);

            Result r5 = new Result(e5, 88.0, "Great socket programming implementation.");
            resultRepository.save(r5);

            log.info("Sample database initialization completed successfully.");
        };
    }
}
