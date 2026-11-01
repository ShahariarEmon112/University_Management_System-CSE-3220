package com.kuet.cse.ums.dto;

import com.kuet.cse.ums.entity.Student;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StudentDto {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String email;

    @NotBlank(message = "Student roll is required")
    private String studentRoll;

    @NotBlank(message = "Department is required")
    private String department;

    @NotNull(message = "Series is required")
    private Integer series;

    private String phoneNumber;

    public StudentDto() {
    }

    public static StudentDto fromEntity(Student student) {
        if (student == null) return null;
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        if (student.getUser() != null) {
            dto.setUserId(student.getUser().getId());
            dto.setUsername(student.getUser().getUsername());
            dto.setFullName(student.getUser().getFullName());
            dto.setEmail(student.getUser().getEmail());
        }
        dto.setStudentRoll(student.getStudentRoll());
        dto.setDepartment(student.getDepartment());
        dto.setSeries(student.getSeries());
        dto.setPhoneNumber(student.getPhoneNumber());
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

    public String getStudentRoll() {
        return studentRoll;
    }

    public void setStudentRoll(String studentRoll) {
        this.studentRoll = studentRoll;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getSeries() {
        return series;
    }

    public void setSeries(Integer series) {
        this.series = series;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
