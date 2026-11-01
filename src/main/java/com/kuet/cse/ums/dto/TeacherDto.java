package com.kuet.cse.ums.dto;

import com.kuet.cse.ums.entity.Teacher;
import jakarta.validation.constraints.NotBlank;

public class TeacherDto {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String email;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Designation is required")
    private String designation;

    private String phoneNumber;

    public TeacherDto() {
    }

    public static TeacherDto fromEntity(Teacher teacher) {
        if (teacher == null) return null;
        TeacherDto dto = new TeacherDto();
        dto.setId(teacher.getId());
        if (teacher.getUser() != null) {
            dto.setUserId(teacher.getUser().getId());
            dto.setUsername(teacher.getUser().getUsername());
            dto.setFullName(teacher.getUser().getFullName());
            dto.setEmail(teacher.getUser().getEmail());
        }
        dto.setDepartment(teacher.getDepartment());
        dto.setDesignation(teacher.getDesignation());
        dto.setPhoneNumber(teacher.getPhoneNumber());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
