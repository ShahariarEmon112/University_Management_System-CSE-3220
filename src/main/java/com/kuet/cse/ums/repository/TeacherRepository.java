package com.kuet.cse.ums.repository;

import com.kuet.cse.ums.entity.Teacher;
import com.kuet.cse.ums.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByUser(User user);
    Optional<Teacher> findByUserUsername(String username);
}
