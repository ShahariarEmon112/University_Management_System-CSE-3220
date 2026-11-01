package com.kuet.cse.ums.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Course code cannot be blank")
    @Column(name = "course_code", nullable = false, unique = true, length = 30)
    private String courseCode; // e.g. CSE 3220

    @NotBlank(message = "Course title cannot be blank")
    @Column(name = "course_title", nullable = false, length = 150)
    private String courseTitle;

    @NotNull(message = "Credits cannot be null")
    @DecimalMin(value = "0.5", message = "Credits must be at least 0.5")
    @Column(nullable = false)
    private Double credits;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher; // assigned instructor (optional or assigned later)

    public Course() {
    }

    public Course(String courseCode, String courseTitle, Double credits, String description, Teacher teacher) {
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.credits = credits;
        this.description = description;
        this.teacher = teacher;
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

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }
}
