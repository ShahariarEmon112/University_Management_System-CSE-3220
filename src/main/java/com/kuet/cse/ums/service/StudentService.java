package com.kuet.cse.ums.service;

import com.kuet.cse.ums.dto.StudentDto;
import com.kuet.cse.ums.entity.Role;
import com.kuet.cse.ums.entity.Student;
import com.kuet.cse.ums.entity.User;
import com.kuet.cse.ums.exception.BadRequestException;
import com.kuet.cse.ums.exception.ResourceNotFoundException;
import com.kuet.cse.ums.exception.UnauthorizedException;
import com.kuet.cse.ums.repository.EnrollmentRepository;
import com.kuet.cse.ums.repository.ResultRepository;
import com.kuet.cse.ums.repository.StudentRepository;
import com.kuet.cse.ums.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ResultRepository resultRepository;

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          EnrollmentRepository enrollmentRepository,
                          ResultRepository resultRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.resultRepository = resultRepository;
    }

    public List<StudentDto> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(StudentDto::fromEntity)
                .collect(Collectors.toList());
    }

    public StudentDto getStudentById(Long id, Authentication authentication) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        // Prevent students from accessing other students' private records
        enforceStudentPrivacy(student, authentication);

        return StudentDto.fromEntity(student);
    }

    public StudentDto getStudentByUsername(String username, Authentication authentication) {
        Student student = studentRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user: " + username));

        enforceStudentPrivacy(student, authentication);

        return StudentDto.fromEntity(student);
    }

    public StudentDto getCurrentStudentProfile(Authentication authentication) {
        return getStudentByUsername(authentication.getName(), authentication);
    }

    @Transactional
    public StudentDto updateStudent(Long id, StudentDto dto, Authentication authentication) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        // Only ADMIN or the student themselves can update their profile
        enforceStudentPrivacy(student, authentication);

        // Department and series are official records: only an admin may change them
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
        if (isAdmin && dto.getDepartment() != null) {
            student.setDepartment(dto.getDepartment());
        }
        if (isAdmin && dto.getSeries() != null) {
            student.setSeries(dto.getSeries());
        }
        if (dto.getPhoneNumber() != null) {
            student.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getFullName() != null && student.getUser() != null) {
            student.getUser().setFullName(dto.getFullName());
        }

        return StudentDto.fromEntity(studentRepository.save(student));
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        // Delete in dependency order: results -> enrollments -> student -> login account
        resultRepository.deleteAll(resultRepository.findByEnrollmentStudentId(student.getId()));
        enrollmentRepository.deleteAll(enrollmentRepository.findByStudentId(student.getId()));
        User user = student.getUser();
        studentRepository.delete(student);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    private void enforceStudentPrivacy(Student student, Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedException("Authentication required");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
        boolean isTeacher = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_TEACHER.name()));

        if (!isAdmin && !isTeacher) {
            // Student role: must match their own username
            String currentUsername = authentication.getName();
            if (student.getUser() == null || !student.getUser().getUsername().equals(currentUsername)) {
                throw new UnauthorizedException("Forbidden: You are not authorized to access another student's record.");
            }
        }
    }
}
