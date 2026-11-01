package com.kuet.cse.ums.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "results")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Enrollment is required")
    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "enrollment_id", referencedColumnName = "id", unique = true, nullable = false)
    private Enrollment enrollment;

    @DecimalMin(value = "0.0", message = "Marks cannot be negative")
    @DecimalMax(value = "100.0", message = "Marks cannot exceed 100")
    @Column(nullable = false)
    private Double marks;

    @Column(name = "letter_grade", nullable = false, length = 5)
    private String letterGrade; // e.g. A+, A, A-, B+, B, C, D, F

    @Column(name = "grade_point", nullable = false)
    private Double gradePoint; // e.g. 4.0, 3.75, 3.5, 0.0

    @Column(length = 255)
    private String remarks;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt = LocalDateTime.now();

    public Result() {
    }

    public Result(Enrollment enrollment, Double marks, String remarks) {
        this.enrollment = enrollment;
        this.setMarksAndComputeGrade(marks);
        this.remarks = remarks;
        this.gradedAt = LocalDateTime.now();
    }

    public void setMarksAndComputeGrade(Double marks) {
        this.marks = marks;
        if (marks >= 80.0) {
            this.letterGrade = "A+";
            this.gradePoint = 4.00;
        } else if (marks >= 75.0) {
            this.letterGrade = "A";
            this.gradePoint = 3.75;
        } else if (marks >= 70.0) {
            this.letterGrade = "A-";
            this.gradePoint = 3.50;
        } else if (marks >= 65.0) {
            this.letterGrade = "B+";
            this.gradePoint = 3.25;
        } else if (marks >= 60.0) {
            this.letterGrade = "B";
            this.gradePoint = 3.00;
        } else if (marks >= 55.0) {
            this.letterGrade = "B-";
            this.gradePoint = 2.75;
        } else if (marks >= 50.0) {
            this.letterGrade = "C+";
            this.gradePoint = 2.50;
        } else if (marks >= 45.0) {
            this.letterGrade = "C";
            this.gradePoint = 2.25;
        } else if (marks >= 40.0) {
            this.letterGrade = "D";
            this.gradePoint = 2.00;
        } else {
            this.letterGrade = "F";
            this.gradePoint = 0.00;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public Double getMarks() {
        return marks;
    }

    public void setMarks(Double marks) {
        setMarksAndComputeGrade(marks);
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
