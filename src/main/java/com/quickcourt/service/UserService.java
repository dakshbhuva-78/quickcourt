package com.quickcourt.service;

import com.quickcourt.config.TokenStore;
import com.quickcourt.dto.LoginRequest;
import com.quickcourt.dto.LoginResponse;
import com.quickcourt.dto.RegisterRequest;
import com.quickcourt.exception.*;
import com.quickcourt.model.*;
import com.quickcourt.repository.UserRepository;
import com.quickcourt.util.FileLogger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Registration, login, logout, profile and admin user-management. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;
    private final FileLogger fileLogger;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TokenStore tokenStore, FileLogger fileLogger) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
        this.fileLogger = fileLogger;
    }

    public User register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new InvalidRequestException("Admin accounts cannot be created through registration");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }
        String hash = passwordEncoder.encode(request.password());
        String name = request.name().trim();

        // The right subclass is created depending on the role (INHERITANCE + POLYMORPHISM)
        User user;
        if (request.role() == Role.VENUE_OWNER) {
            user = new VenueOwner(name, email, hash);
        } else {
            user = new Player(name, email, hash);
        }
        User saved = userRepository.save(user);
        fileLogger.info("New " + saved.getRole() + " registered: " + email);
        return saved;
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        // Same message for "no such user" and "wrong password" so attackers cannot guess which emails exist
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            fileLogger.warn("Failed login for " + email);
            throw new AuthenticationFailedException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new ForbiddenException("This account has been banned by the admin");
        }
        String token = tokenStore.create(user.getId());
        fileLogger.info("Login: " + email + " (" + user.getRole() + ")");
        return new LoginResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getDashboardPath());
    }

    public void logout(String token) {
        tokenStore.remove(token);
    }

    public User updateName(User current, String newName) {
        current.setName(newName.trim());
        return userRepository.save(current);
    }

    // ---------- admin features ----------

    public List<User> searchUsers(Role role, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAll().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> q.isEmpty() || u.getName().toLowerCase(Locale.ROOT).contains(q)
                        || u.getEmail().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());
    }

    public User setActive(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        if (user.getRole() == Role.ADMIN) {
            throw new InvalidRequestException("Admin accounts cannot be banned");
        }
        user.setActive(active);
        fileLogger.info("Admin " + (active ? "unbanned " : "banned ") + user.getEmail());
        return userRepository.save(user);
    }
}
