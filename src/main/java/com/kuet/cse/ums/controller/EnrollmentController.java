package com.kuet.cse.ums.controller;

import com.kuet.cse.ums.dto.EnrollmentDto;
import com.kuet.cse.ums.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EnrollmentDto>> getAllEnrollments() {
        return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<EnrollmentDto>> getMyEnrollments(Authentication authentication) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsForCurrentStudent(authentication));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<EnrollmentDto>> getEnrollmentsByCourse(@PathVariable Long courseId,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByCourse(courseId, authentication));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnrollmentDto> enrollStudent(@Valid @RequestBody EnrollmentDto dto) {
        return new ResponseEntity<>(enrollmentService.enrollStudent(dto), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteEnrollment(@PathVariable Long id) {
        enrollmentService.deleteEnrollment(id);
        return ResponseEntity.ok(Map.of("message", "Enrollment deleted successfully"));
    }
}
