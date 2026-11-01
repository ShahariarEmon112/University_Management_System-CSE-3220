package com.kuet.cse.ums.service;

import com.kuet.cse.ums.dto.CourseDto;
import com.kuet.cse.ums.entity.Course;
import com.kuet.cse.ums.entity.Teacher;
import com.kuet.cse.ums.exception.BadRequestException;
import com.kuet.cse.ums.exception.ResourceNotFoundException;
import com.kuet.cse.ums.repository.CourseRepository;
import com.kuet.cse.ums.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;

    public CourseService(CourseRepository courseRepository, TeacherRepository teacherRepository) {
        this.courseRepository = courseRepository;
        this.teacherRepository = teacherRepository;
    }

    public List<CourseDto> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(CourseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public CourseDto getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        return CourseDto.fromEntity(course);
    }

    public List<CourseDto> getCoursesByTeacher(String username) {
        Teacher teacher = teacherRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with username: " + username));
        return courseRepository.findByTeacher(teacher).stream()
                .map(CourseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CourseDto createCourse(CourseDto dto) {
        if (courseRepository.existsByCourseCode(dto.getCourseCode())) {
            throw new BadRequestException("Course code '" + dto.getCourseCode() + "' already exists.");
        }

        Course course = new Course();
        course.setCourseCode(dto.getCourseCode());
        course.setCourseTitle(dto.getCourseTitle());
        course.setCredits(dto.getCredits());
        course.setDescription(dto.getDescription());

        if (dto.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + dto.getTeacherId()));
            course.setTeacher(teacher);
        }

        return CourseDto.fromEntity(courseRepository.save(course));
    }

    @Transactional
    public CourseDto updateCourse(Long id, CourseDto dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        if (dto.getCourseTitle() != null) course.setCourseTitle(dto.getCourseTitle());
        if (dto.getCredits() != null) course.setCredits(dto.getCredits());
        if (dto.getDescription() != null) course.setDescription(dto.getDescription());

        if (dto.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + dto.getTeacherId()));
            course.setTeacher(teacher);
        }

        return CourseDto.fromEntity(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        courseRepository.delete(course);
    }
}
