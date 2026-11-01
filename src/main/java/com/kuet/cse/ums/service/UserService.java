package com.kuet.cse.ums.service;

import com.kuet.cse.ums.dto.LoginRequestDto;
import com.kuet.cse.ums.dto.RegisterRequestDto;
import com.kuet.cse.ums.dto.UserResponseDto;
import com.kuet.cse.ums.entity.Role;
import com.kuet.cse.ums.entity.Student;
import com.kuet.cse.ums.entity.Teacher;
import com.kuet.cse.ums.entity.User;
import com.kuet.cse.ums.exception.BadRequestException;
import com.kuet.cse.ums.exception.ResourceNotFoundException;
import com.kuet.cse.ums.repository.StudentRepository;
import com.kuet.cse.ums.repository.TeacherRepository;
import com.kuet.cse.ums.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       TeacherRepository teacherRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        // Automatically create associated Student or Teacher record if role is STUDENT or TEACHER
        if (request.getRole() == Role.ROLE_STUDENT) {
            String roll = request.getStudentRoll() != null ? request.getStudentRoll() : "S-" + savedUser.getId();
            if (studentRepository.existsByStudentRoll(roll)) {
                throw new BadRequestException("Student roll '" + roll + "' is already registered.");
            }
            Student student = new Student(
                    savedUser,
                    roll,
                    request.getDepartment() != null ? request.getDepartment() : "Computer Science & Engineering",
                    request.getSeries() != null ? request.getSeries() : 2022,
                    request.getPhoneNumber()
            );
            studentRepository.save(student);
        } else if (request.getRole() == Role.ROLE_TEACHER) {
            Teacher teacher = new Teacher(
                    savedUser,
                    request.getDepartment() != null ? request.getDepartment() : "Computer Science & Engineering",
                    request.getDesignation() != null ? request.getDesignation() : "Assistant Professor",
                    request.getPhoneNumber()
            );
            teacherRepository.save(teacher);
        }

        return UserResponseDto.fromEntity(savedUser);
    }

    public UserResponseDto login(LoginRequestDto loginRequest, HttpServletRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(auth);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserResponseDto.fromEntity(user);
    }

    public UserResponseDto getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("No authenticated user in current session");
        }
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
        return UserResponseDto.fromEntity(user);
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponseDto.fromEntity(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }
}
