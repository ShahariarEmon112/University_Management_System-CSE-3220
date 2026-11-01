package com.kuet.cse.ums.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true, nullable = false)
    private User user;

    @NotBlank(message = "Student roll cannot be blank")
    @Column(name = "student_roll", nullable = false, unique = true, length = 30)
    private String studentRoll;

    @NotBlank(message = "Department cannot be blank")
    @Column(nullable = false, length = 100)
    private String department;

    @NotNull(message = "Series/Year cannot be null")
    @Column(nullable = false)
    private Integer series; // e.g. 2021, 2022

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    public Student() {
    }

    public Student(User user, String studentRoll, String department, Integer series, String phoneNumber) {
        this.user = user;
        this.studentRoll = studentRoll;
        this.department = department;
        this.series = series;
        this.phoneNumber = phoneNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
