package com.kuet.cse.ums.dto;

import com.kuet.cse.ums.entity.Enrollment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class EnrollmentDto {
    private Long id;

    @NotNull(message = "Student ID is required")
    private Long studentId;
    private String studentName;
    private String studentRoll;

    @NotNull(message = "Course ID is required")
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private Double credits;
    private String teacherName;

    @NotBlank(message = "Semester is required")
    private String semester;

    private LocalDate enrollmentDate;

    public EnrollmentDto() {
    }

    public static EnrollmentDto fromEntity(Enrollment enrollment) {
        if (enrollment == null) return null;
        EnrollmentDto dto = new EnrollmentDto();
        dto.setId(enrollment.getId());
        if (enrollment.getStudent() != null) {
            dto.setStudentId(enrollment.getStudent().getId());
            dto.setStudentRoll(enrollment.getStudent().getStudentRoll());
            if (enrollment.getStudent().getUser() != null) {
                dto.setStudentName(enrollment.getStudent().getUser().getFullName());
            }
        }
        if (enrollment.getCourse() != null) {
            dto.setCourseId(enrollment.getCourse().getId());
            dto.setCourseCode(enrollment.getCourse().getCourseCode());
            dto.setCourseTitle(enrollment.getCourse().getCourseTitle());
            dto.setCredits(enrollment.getCourse().getCredits());
            if (enrollment.getCourse().getTeacher() != null && enrollment.getCourse().getTeacher().getUser() != null) {
                dto.setTeacherName(enrollment.getCourse().getTeacher().getUser().getFullName());
            }
        }
        dto.setSemester(enrollment.getSemester());
        dto.setEnrollmentDate(enrollment.getEnrollmentDate());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentRoll() {
        return studentRoll;
    }

    public void setStudentRoll(String studentRoll) {
        this.studentRoll = studentRoll;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public Double getCredits() {
        return credits;
    }

    public void setCredits(Double credits) {
        this.credits = credits;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }
}
