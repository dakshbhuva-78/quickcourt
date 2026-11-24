package com.quickcourt.service;

import com.quickcourt.config.TokenStore;
import com.quickcourt.dto.LoginRequest;
import com.quickcourt.dto.LoginResponse;
import com.quickcourt.dto.RegisterRequest;
import com.quickcourt.exception.*;
import com.quickcourt.model.*;
import com.quickcourt.repository.UserRepository;
import com.quickcourt.util.FileLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Unit tests for registration, login and admin user actions (repository is mocked). */
class UserServiceTest {

    private UserRepository userRepository;
    private BCryptPasswordEncoder encoder;
    private UserService service;

    @BeforeEach
    void setUp(@TempDir Path tmp) {
        userRepository = Mockito.mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder();
        service = new UserService(userRepository, encoder, new TokenStore(), new FileLogger(tmp.toString()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registersPlayerWithHashedPassword() {
        User u = service.register(new RegisterRequest("Aarav", "Aarav@Mail.com", "Strong@123", Role.PLAYER));
        assertInstanceOf(Player.class, u);
        assertEquals("aarav@mail.com", u.getEmail());          // email lower-cased
        assertNotEquals("Strong@123", u.getPassword());         // never plain text
        assertTrue(encoder.matches("Strong@123", u.getPassword()));
    }

    @Test
    void registersVenueOwnerSubclass() {
        User u = service.register(new RegisterRequest("Rahul", "rahul@mail.com", "Strong@123", Role.VENUE_OWNER));
        assertInstanceOf(VenueOwner.class, u);
        assertEquals("owner-dashboard.html", u.getDashboardPath());
    }

    @Test
    void duplicateEmailIsRejected() {
        when(userRepository.existsByEmail("dup@mail.com")).thenReturn(true);
        assertThrows(UserAlreadyExistsException.class,
                () -> service.register(new RegisterRequest("Dup", "dup@mail.com", "Strong@123", Role.PLAYER)));
    }

    @Test
    void adminCannotSelfRegister() {
        assertThrows(InvalidRequestException.class,
                () -> service.register(new RegisterRequest("Evil", "evil@mail.com", "Strong@123", Role.ADMIN)));
    }

    @Test
    void validLoginReturnsToken() {
        Player p = new Player("Aarav", "a@mail.com", encoder.encode("Strong@123"));
        p.setId(7L);
        when(userRepository.findByEmail("a@mail.com")).thenReturn(Optional.of(p));
        LoginResponse r = service.login(new LoginRequest("a@mail.com", "Strong@123"));
        assertNotNull(r.token());
        assertEquals(Role.PLAYER, r.role());
        assertEquals("venues.html", r.dashboard());
    }

    @Test
    void wrongPasswordIsRejected() {
        Player p = new Player("Aarav", "a@mail.com", encoder.encode("Strong@123"));
        when(userRepository.findByEmail("a@mail.com")).thenReturn(Optional.of(p));
        assertThrows(AuthenticationFailedException.class, () -> service.login(new LoginRequest("a@mail.com", "Wrong@123")));
    }

    @Test
    void unknownEmailGivesSameError() {
        when(userRepository.findByEmail("none@mail.com")).thenReturn(Optional.empty());
        AuthenticationFailedException ex = assertThrows(AuthenticationFailedException.class,
                () -> service.login(new LoginRequest("none@mail.com", "Strong@123")));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void bannedUserCannotLogin() {
        Player p = new Player("Aarav", "a@mail.com", encoder.encode("Strong@123"));
        p.setActive(false);
        when(userRepository.findByEmail("a@mail.com")).thenReturn(Optional.of(p));
        assertThrows(ForbiddenException.class, () -> service.login(new LoginRequest("a@mail.com", "Strong@123")));
    }

    @Test
    void adminCannotBeBanned() {
        Admin admin = new Admin("Admin", "admin@mail.com", "hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        assertThrows(InvalidRequestException.class, () -> service.setActive(1L, false));
    }

    @Test
    void banAndUnbanPlayer() {
        Player p = new Player("Aarav", "a@mail.com", "hash");
        when(userRepository.findById(2L)).thenReturn(Optional.of(p));
        assertFalse(service.setActive(2L, false).isActive());
        assertTrue(service.setActive(2L, true).isActive());
    }

    @Test
    void searchFiltersByRoleAndText() {
        Player p = new Player("Aarav Mehta", "aarav@mail.com", "h");
        VenueOwner o = new VenueOwner("Rahul Patel", "rahul@mail.com", "h");
        when(userRepository.findAll()).thenReturn(List.of(p, o));
        assertEquals(1, service.searchUsers(Role.PLAYER, null).size());
        assertEquals(1, service.searchUsers(null, "patel").size());
        assertEquals(2, service.searchUsers(null, "").size());
    }
}
