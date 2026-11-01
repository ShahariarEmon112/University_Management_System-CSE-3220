package com.kuet.cse.ums.repository;

import com.kuet.cse.ums.entity.Course;
import com.kuet.cse.ums.entity.Enrollment;
import com.kuet.cse.ums.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(Student student);
    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findByCourse(Course course);
    List<Enrollment> findByCourseId(Long courseId);
    List<Enrollment> findByCourseTeacherId(Long teacherId);
    Optional<Enrollment> findByStudentAndCourseAndSemester(Student student, Course course, String semester);
    boolean existsByStudentIdAndCourseIdAndSemester(Long studentId, Long courseId, String semester);
}
