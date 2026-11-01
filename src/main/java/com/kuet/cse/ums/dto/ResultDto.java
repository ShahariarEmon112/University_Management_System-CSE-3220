package com.kuet.cse.ums.dto;

import com.kuet.cse.ums.entity.Result;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class ResultDto {
    private Long id;

    @NotNull(message = "Enrollment ID is required")
    private Long enrollmentId;

    private Long studentId;
    private String studentRoll;
    private String studentName;

    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private Double credits;
    private String semester;

    @NotNull(message = "Marks are required")
    @DecimalMin(value = "0.0", message = "Marks cannot be negative")
    @DecimalMax(value = "100.0", message = "Marks cannot exceed 100")
    private Double marks;

    private String letterGrade;
    private Double gradePoint;
    private String remarks;
    private LocalDateTime gradedAt;

    public ResultDto() {
    }

    public static ResultDto fromEntity(Result result) {
        if (result == null) return null;
        ResultDto dto = new ResultDto();
        dto.setId(result.getId());
        if (result.getEnrollment() != null) {
            dto.setEnrollmentId(result.getEnrollment().getId());
            if (result.getEnrollment().getStudent() != null) {
                dto.setStudentId(result.getEnrollment().getStudent().getId());
                dto.setStudentRoll(result.getEnrollment().getStudent().getStudentRoll());
                if (result.getEnrollment().getStudent().getUser() != null) {
                    dto.setStudentName(result.getEnrollment().getStudent().getUser().getFullName());
                }
            }
            if (result.getEnrollment().getCourse() != null) {
                dto.setCourseId(result.getEnrollment().getCourse().getId());
                dto.setCourseCode(result.getEnrollment().getCourse().getCourseCode());
                dto.setCourseTitle(result.getEnrollment().getCourse().getCourseTitle());
                dto.setCredits(result.getEnrollment().getCourse().getCredits());
            }
            dto.setSemester(result.getEnrollment().getSemester());
        }
        dto.setMarks(result.getMarks());
        dto.setLetterGrade(result.getLetterGrade());
        dto.setGradePoint(result.getGradePoint());
        dto.setRemarks(result.getRemarks());
        dto.setGradedAt(result.getGradedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentRoll() {
        return studentRoll;
    }

    public void setStudentRoll(String studentRoll) {
        this.studentRoll = studentRoll;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
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

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Double getMarks() {
        return marks;
    }

    public void setMarks(Double marks) {
        this.marks = marks;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public void setLetterGrade(String letterGrade) {
        this.letterGrade = letterGrade;
    }

    public Double getGradePoint() {
        return gradePoint;
    }

    public void setGradePoint(Double gradePoint) {
        this.gradePoint = gradePoint;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }
}
