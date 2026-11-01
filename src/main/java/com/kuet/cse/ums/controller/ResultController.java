package com.kuet.cse.ums.controller;

import com.kuet.cse.ums.dto.ResultDto;
import com.kuet.cse.ums.service.ResultService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ResultDto>> getAllResults() {
        return ResponseEntity.ok(resultService.getAllResults());
    }

    @GetMapping("/my-results")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ResultDto>> getMyResults(Authentication authentication) {
        return ResponseEntity.ok(resultService.getResultsForCurrentStudent(authentication));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<ResultDto>> getResultsByStudent(@PathVariable Long studentId, Authentication authentication) {
        return ResponseEntity.ok(resultService.getResultsByStudentId(studentId, authentication));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<ResultDto>> getResultsByCourse(@PathVariable Long courseId, Authentication authentication) {
        return ResponseEntity.ok(resultService.getResultsByCourseId(courseId, authentication));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ResultDto> saveOrUpdateResult(@Valid @RequestBody ResultDto dto, Authentication authentication) {
        return ResponseEntity.ok(resultService.saveOrUpdateResult(dto, authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteResult(@PathVariable Long id) {
        resultService.deleteResult(id);
        return ResponseEntity.ok(Map.of("message", "Result deleted successfully"));
    }
}
