package com.kuet.cse.ums.service;

import com.kuet.cse.ums.dto.TeacherDto;
import com.kuet.cse.ums.entity.Teacher;
import com.kuet.cse.ums.exception.ResourceNotFoundException;
import com.kuet.cse.ums.repository.TeacherRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
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
    public TeacherDto updateTeacher(Long id, TeacherDto dto) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

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
        teacherRepository.delete(teacher);
    }
}
