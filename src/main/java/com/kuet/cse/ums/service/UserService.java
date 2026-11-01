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
import com.kuet.cse.ums.exception.UnauthorizedException;
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
    private final StudentService studentService;
    private final TeacherService teacherService;

    public UserService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       TeacherRepository teacherRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       StudentService studentService,
                       TeacherService teacherService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.studentService = studentService;
        this.teacherService = teacherService;
    }

    /** Registration used by trusted code (seeding, tests, admins). Any role is allowed. */
    @Transactional
    public UserResponseDto register(RegisterRequestDto request) {
        return createAccount(request);
    }

    /**
     * Registration from the public endpoint. Only an authenticated admin may create
     * TEACHER or ADMIN accounts; everyone else is registered as a STUDENT.
     */
    @Transactional
    public UserResponseDto register(RegisterRequestDto request, Authentication authentication) {
        boolean callerIsAdmin = authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
        if (!callerIsAdmin && request.getRole() != Role.ROLE_STUDENT) {
            throw new UnauthorizedException("Only an administrator can create " + request.getRole().name().replace("ROLE_", "") + " accounts.");
        }
        return createAccount(request);
    }

    private UserResponseDto createAccount(RegisterRequestDto request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username '" + username + "' is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email '" + email + "' is already registered.");
        }
        String roll = isBlank(request.getStudentRoll()) ? null : request.getStudentRoll().trim();
        if (request.getRole() == Role.ROLE_STUDENT && roll != null && studentRepository.existsByStudentRoll(roll)) {
            throw new BadRequestException("Student roll '" + roll + "' is already registered.");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setRole(request.getRole());
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        String department = isBlank(request.getDepartment()) ? "Computer Science & Engineering" : request.getDepartment().trim();

        // Automatically create associated Student or Teacher record if role is STUDENT or TEACHER
        if (request.getRole() == Role.ROLE_STUDENT) {
            Student student = new Student(
                    savedUser,
                    roll != null ? roll : "S-" + savedUser.getId(),
                    department,
                    request.getSeries() != null ? request.getSeries() : 2022,
                    request.getPhoneNumber()
            );
            studentRepository.save(student);
        } else if (request.getRole() == Role.ROLE_TEACHER) {
            Teacher teacher = new Teacher(
                    savedUser,
                    department,
                    isBlank(request.getDesignation()) ? "Assistant Professor" : request.getDesignation().trim(),
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

        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);

        // Issue a fresh session id on login to prevent session fixation
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
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
    public void deleteUser(Long id, Authentication authentication) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (authentication != null && user.getUsername().equals(authentication.getName())) {
            throw new BadRequestException("You cannot delete your own account while logged in.");
        }

        // Student/teacher profiles reference the user, so remove them (and their data) first
        var student = studentRepository.findByUser(user);
        if (student.isPresent()) {
            studentService.deleteStudent(student.get().getId());
            return;
        }
        var teacher = teacherRepository.findByUser(user);
        if (teacher.isPresent()) {
            teacherService.deleteTeacher(teacher.get().getId());
            return;
        }
        userRepository.delete(user);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
