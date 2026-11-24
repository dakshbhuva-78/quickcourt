package com.quickcourt.controller;

import com.quickcourt.dto.LoginRequest;
import com.quickcourt.dto.LoginResponse;
import com.quickcourt.dto.ProfileUpdateRequest;
import com.quickcourt.dto.RegisterRequest;
import com.quickcourt.model.User;
import com.quickcourt.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Authentication and profile endpoints. */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return Map.of("message", "Registration successful. Please log in.", "id", user.getId());
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/auth/logout")
    public Map<String, String> logout(@RequestHeader(value = "Authorization", required = false) String header) {
        if (header != null && header.startsWith("Bearer ")) {
            userService.logout(header.substring(7));
        }
        return Map.of("message", "Logged out");
    }

    @GetMapping("/users/me")
    public User me(@RequestAttribute("currentUser") User currentUser) {
        return currentUser;
    }

    @PutMapping("/users/me")
    public User updateMe(@RequestAttribute("currentUser") User currentUser,
                         @Valid @RequestBody ProfileUpdateRequest request) {
        return userService.updateName(currentUser, request.name());
    }
}
