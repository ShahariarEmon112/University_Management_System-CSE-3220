package com.kuet.cse.ums.repository;

import com.kuet.cse.ums.entity.Course;
import com.kuet.cse.ums.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    boolean existsByCourseCode(String courseCode);
    List<Course> findByTeacher(Teacher teacher);
    List<Course> findByTeacherId(Long teacherId);
}
