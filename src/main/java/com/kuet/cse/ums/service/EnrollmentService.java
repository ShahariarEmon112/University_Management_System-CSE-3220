package com.kuet.cse.ums.service;

import com.kuet.cse.ums.dto.EnrollmentDto;
import com.kuet.cse.ums.entity.Course;
import com.kuet.cse.ums.entity.Enrollment;
import com.kuet.cse.ums.entity.Role;
import com.kuet.cse.ums.entity.Student;
import com.kuet.cse.ums.exception.BadRequestException;
import com.kuet.cse.ums.exception.ResourceNotFoundException;
import com.kuet.cse.ums.exception.UnauthorizedException;
import com.kuet.cse.ums.repository.CourseRepository;
import com.kuet.cse.ums.repository.EnrollmentRepository;
import com.kuet.cse.ums.repository.StudentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository,
                             CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    public List<EnrollmentDto> getAllEnrollments() {
        return enrollmentRepository.findAll().stream()
                .map(EnrollmentDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EnrollmentDto> getEnrollmentsForCurrentStudent(Authentication authentication) {
        Student student = studentRepository.findByUserUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user: " + authentication.getName()));
        return enrollmentRepository.findByStudent(student).stream()
                .map(EnrollmentDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EnrollmentDto> getEnrollmentsByCourse(Long courseId, Authentication authentication) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        // Teachers can view enrollments for courses they teach; Admins can view any
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
        if (!isAdmin) {
            if (course.getTeacher() == null || !course.getTeacher().getUser().getUsername().equals(authentication.getName())) {
                throw new UnauthorizedException("Forbidden: You can only view student enrollments for courses you teach.");
            }
        }

        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(EnrollmentDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public EnrollmentDto enrollStudent(EnrollmentDto dto) {
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + dto.getStudentId()));
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + dto.getCourseId()));

        if (enrollmentRepository.existsByStudentIdAndCourseIdAndSemester(student.getId(), course.getId(), dto.getSemester())) {
            throw new BadRequestException("Student is already enrolled in " + course.getCourseCode() + " for semester " + dto.getSemester());
        }

        Enrollment enrollment = new Enrollment(student, course, dto.getSemester());
        return EnrollmentDto.fromEntity(enrollmentRepository.save(enrollment));
    }

    @Transactional
    public void deleteEnrollment(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
        enrollmentRepository.delete(enrollment);
    }
}
