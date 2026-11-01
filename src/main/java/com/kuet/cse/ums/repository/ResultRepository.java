package com.kuet.cse.ums.repository;

import com.kuet.cse.ums.entity.Enrollment;
import com.kuet.cse.ums.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    Optional<Result> findByEnrollment(Enrollment enrollment);
    Optional<Result> findByEnrollmentId(Long enrollmentId);
    List<Result> findByEnrollmentStudentId(Long studentId);
    List<Result> findByEnrollmentCourseId(Long courseId);
    boolean existsByEnrollmentId(Long enrollmentId);
}
