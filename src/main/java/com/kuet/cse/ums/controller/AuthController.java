package com.kuet.cse.ums.controller;

import com.kuet.cse.ums.dto.LoginRequestDto;
import com.kuet.cse.ums.dto.RegisterRequestDto;
import com.kuet.cse.ums.dto.UserResponseDto;
import com.kuet.cse.ums.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        UserResponseDto created = userService.register(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest servletRequest) {
        UserResponseDto response = userService.login(request, servletRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserResponseDto currentUser = userService.getCurrentUser(authentication);
        return ResponseEntity.ok(currentUser);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities()
        ));
    }
}
