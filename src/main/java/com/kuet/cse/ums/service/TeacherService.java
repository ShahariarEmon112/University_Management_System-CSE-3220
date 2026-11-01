package com.kuet.cse.ums.service;

import com.kuet.cse.ums.dto.TeacherDto;
import com.kuet.cse.ums.entity.Course;
import com.kuet.cse.ums.entity.Role;
import com.kuet.cse.ums.entity.Teacher;
import com.kuet.cse.ums.entity.User;
import com.kuet.cse.ums.exception.ResourceNotFoundException;
import com.kuet.cse.ums.exception.UnauthorizedException;
import com.kuet.cse.ums.repository.CourseRepository;
import com.kuet.cse.ums.repository.TeacherRepository;
import com.kuet.cse.ums.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public TeacherService(TeacherRepository teacherRepository,
                          CourseRepository courseRepository,
                          UserRepository userRepository) {
        this.teacherRepository = teacherRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    public List<TeacherDto> getAllTeachers() {
        return teacherRepository.findAll().stream()
                .map(TeacherDto::fromEntity)
                .collect(Collectors.toList());
    }

    public TeacherDto getTeacherById(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
        return TeacherDto.fromEntity(teacher);
    }

    public TeacherDto getCurrentTeacherProfile(Authentication authentication) {
        Teacher teacher = teacherRepository.findByUserUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user: " + authentication.getName()));
        return TeacherDto.fromEntity(teacher);
    }

    @Transactional
    public TeacherDto updateTeacher(Long id, TeacherDto dto, Authentication authentication) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        // A teacher may only edit their own profile; admins may edit anyone
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
        if (!isAdmin && (teacher.getUser() == null
                || !teacher.getUser().getUsername().equals(authentication.getName()))) {
            throw new UnauthorizedException("Forbidden: You can only update your own teacher profile.");
        }

        if (dto.getDepartment() != null) teacher.setDepartment(dto.getDepartment());
        if (dto.getDesignation() != null) teacher.setDesignation(dto.getDesignation());
        if (dto.getPhoneNumber() != null) teacher.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getFullName() != null && teacher.getUser() != null) {
            teacher.getUser().setFullName(dto.getFullName());
        }

        return TeacherDto.fromEntity(teacherRepository.save(teacher));
    }

    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
        // Keep the teacher's courses, but mark them unassigned
        for (Course course : courseRepository.findByTeacherId(teacher.getId())) {
            course.setTeacher(null);
            courseRepository.save(course);
        }
        User user = teacher.getUser();
        teacherRepository.delete(teacher);
        if (user != null) {
            userRepository.delete(user);
        }
    }
}
