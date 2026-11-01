package com.kuet.cse.ums.service;

import com.kuet.cse.ums.dto.ResultDto;
import com.kuet.cse.ums.entity.Course;
import com.kuet.cse.ums.entity.Enrollment;
import com.kuet.cse.ums.entity.Result;
import com.kuet.cse.ums.entity.Role;
import com.kuet.cse.ums.entity.Student;
import com.kuet.cse.ums.exception.BadRequestException;
import com.kuet.cse.ums.exception.ResourceNotFoundException;
import com.kuet.cse.ums.exception.UnauthorizedException;
import com.kuet.cse.ums.repository.CourseRepository;
import com.kuet.cse.ums.repository.EnrollmentRepository;
import com.kuet.cse.ums.repository.ResultRepository;
import com.kuet.cse.ums.repository.StudentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResultService {

    private final ResultRepository resultRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public ResultService(ResultRepository resultRepository,
                         EnrollmentRepository enrollmentRepository,
                         StudentRepository studentRepository,
                         CourseRepository courseRepository) {
        this.resultRepository = resultRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    public List<ResultDto> getAllResults() {
        return resultRepository.findAll().stream()
                .map(ResultDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ResultDto> getResultsForCurrentStudent(Authentication authentication) {
        Student student = studentRepository.findByUserUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user: " + authentication.getName()));
        return resultRepository.findByEnrollmentStudentId(student.getId()).stream()
                .map(ResultDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ResultDto> getResultsByStudentId(Long studentId, Authentication authentication) {
        // Enforce student privacy
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
        boolean isTeacher = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_TEACHER.name()));

        if (!isAdmin && !isTeacher) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
            if (student.getUser() == null || !student.getUser().getUsername().equals(authentication.getName())) {
                throw new UnauthorizedException("Forbidden: You cannot view results of another student.");
            }
        }

        return resultRepository.findByEnrollmentStudentId(studentId).stream()
                .map(ResultDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ResultDto> getResultsByCourseId(Long courseId, Authentication authentication) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        // Teachers may only see grades for courses they teach
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
        if (!isAdmin && (course.getTeacher() == null
                || !course.getTeacher().getUser().getUsername().equals(authentication.getName()))) {
            throw new UnauthorizedException("Forbidden: You can only view grades for courses you teach.");
        }

        return resultRepository.findByEnrollmentCourseId(courseId).stream()
                .map(ResultDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ResultDto saveOrUpdateResult(ResultDto dto, Authentication authentication) {
        Enrollment enrollment = enrollmentRepository.findById(dto.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + dto.getEnrollmentId()));

        // Verification: Only Admin or the assigned Course Teacher can grade
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));

        if (!isAdmin) {
            if (enrollment.getCourse().getTeacher() == null ||
                !enrollment.getCourse().getTeacher().getUser().getUsername().equals(authentication.getName())) {
                throw new UnauthorizedException("Forbidden: You can only assign grades for courses you are teaching.");
            }
        }

        Result result = resultRepository.findByEnrollmentId(enrollment.getId())
                .orElse(new Result());

        result.setEnrollment(enrollment);
        result.setMarksAndComputeGrade(dto.getMarks());
        result.setRemarks(dto.getRemarks());
        result.setGradedAt(LocalDateTime.now());

        return ResultDto.fromEntity(resultRepository.save(result));
    }

    @Transactional
    public void deleteResult(Long id) {
        Result result = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found with id: " + id));
        resultRepository.delete(result);
    }
}
