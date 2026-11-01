package com.kuet.cse.ums.repository;

import com.kuet.cse.ums.entity.Student;
import com.kuet.cse.ums.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUser(User user);
    Optional<Student> findByUserUsername(String username);
    Optional<Student> findByStudentRoll(String studentRoll);
    boolean existsByStudentRoll(String studentRoll);
}
