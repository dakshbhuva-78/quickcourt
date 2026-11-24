package com.quickcourt.config;

import com.quickcourt.exception.AuthenticationFailedException;
import com.quickcourt.exception.ForbiddenException;
import com.quickcourt.model.Role;
import com.quickcourt.model.User;
import com.quickcourt.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs BEFORE every /api/** controller method.
 *  1. Public URLs (login, register, browsing venues) pass straight through.
 *  2. Every other URL needs a valid token -> otherwise 401.
 *  3. ROLE-BASED ACCESS: /api/admin/** only for ADMIN, /api/owner/** only for VENUE_OWNER.
 *  4. The logged-in user is stored in the request so controllers can read it.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER = "currentUser";

    private final TokenStore tokenStore;
    private final UserRepository userRepository;

    public AuthInterceptor(TokenStore tokenStore, UserRepository userRepository) {
        this.tokenStore = tokenStore;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equals(method) || isPublic(path, method)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AuthenticationFailedException("Please log in first");
        }
        Long userId = tokenStore.validate(header.substring(7))
                .orElseThrow(() -> new AuthenticationFailedException("Session expired. Please log in again"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("User no longer exists"));

        if (!user.isActive()) {
            throw new ForbiddenException("This account has been banned by the admin");
        }
        if (path.startsWith("/api/admin") && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Admin access only");
        }
        if (path.startsWith("/api/owner") && user.getRole() != Role.VENUE_OWNER) {
            throw new ForbiddenException("Venue owner access only");
        }

        request.setAttribute(CURRENT_USER, user);
        return true;
    }

    private boolean isPublic(String path, String method) {
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            return true;
        }
        // anyone may browse venues, courts and slots (GET only)
        return "GET".equals(method) && path.startsWith("/api/venues");
    }
}
