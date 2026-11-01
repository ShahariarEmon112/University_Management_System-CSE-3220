package com.kuet.cse.ums.dto;

import com.kuet.cse.ums.entity.Course;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CourseDto {
    private Long id;

    @NotBlank(message = "Course code cannot be blank")
    private String courseCode;

    @NotBlank(message = "Course title cannot be blank")
    private String courseTitle;

    @NotNull(message = "Credits cannot be null")
    @DecimalMin(value = "0.5", message = "Credits must be at least 0.5")
    private Double credits;

    private String description;
    private Long teacherId;
    private String teacherName;

    public CourseDto() {
    }

    public static CourseDto fromEntity(Course course) {
        if (course == null) return null;
        CourseDto dto = new CourseDto();
        dto.setId(course.getId());
        dto.setCourseCode(course.getCourseCode());
        dto.setCourseTitle(course.getCourseTitle());
        dto.setCredits(course.getCredits());
        dto.setDescription(course.getDescription());
        if (course.getTeacher() != null) {
            dto.setTeacherId(course.getTeacher().getId());
            if (course.getTeacher().getUser() != null) {
                dto.setTeacherName(course.getTeacher().getUser().getFullName());
            }
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }
}
