package com.kuet.cse.ums.service;

import com.kuet.cse.ums.dto.CourseDto;
import com.kuet.cse.ums.entity.Course;
import com.kuet.cse.ums.entity.Teacher;
import com.kuet.cse.ums.exception.BadRequestException;
import com.kuet.cse.ums.exception.ResourceNotFoundException;
import com.kuet.cse.ums.repository.CourseRepository;
import com.kuet.cse.ums.repository.EnrollmentRepository;
import com.kuet.cse.ums.repository.ResultRepository;
import com.kuet.cse.ums.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ResultRepository resultRepository;

    public CourseService(CourseRepository courseRepository,
                         TeacherRepository teacherRepository,
                         EnrollmentRepository enrollmentRepository,
                         ResultRepository resultRepository) {
        this.courseRepository = courseRepository;
        this.teacherRepository = teacherRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.resultRepository = resultRepository;
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
        String code = dto.getCourseCode().trim().toUpperCase();
        if (courseRepository.existsByCourseCode(code)) {
            throw new BadRequestException("Course code '" + code + "' already exists.");
        }

        Course course = new Course();
        course.setCourseCode(code);
        course.setCourseTitle(dto.getCourseTitle().trim());
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

        if (dto.getCourseCode() != null) {
            String code = dto.getCourseCode().trim().toUpperCase();
            if (!code.equals(course.getCourseCode()) && courseRepository.existsByCourseCode(code)) {
                throw new BadRequestException("Course code '" + code + "' already exists.");
            }
            course.setCourseCode(code);
        }
        if (dto.getCourseTitle() != null) course.setCourseTitle(dto.getCourseTitle().trim());
        if (dto.getCredits() != null) course.setCredits(dto.getCredits());
        if (dto.getDescription() != null) course.setDescription(dto.getDescription());

        // A full update replaces the teacher: null means "unassigned"
        if (dto.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + dto.getTeacherId()));
            course.setTeacher(teacher);
        } else {
            course.setTeacher(null);
        }

        return CourseDto.fromEntity(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        // Remove dependent rows first, otherwise the foreign keys block the delete
        resultRepository.deleteAll(resultRepository.findByEnrollmentCourseId(course.getId()));
        enrollmentRepository.deleteAll(enrollmentRepository.findByCourseId(course.getId()));
        courseRepository.delete(course);
    }
}
