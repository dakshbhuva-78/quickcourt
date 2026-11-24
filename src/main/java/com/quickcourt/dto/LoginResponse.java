package com.quickcourt.dto;

import com.quickcourt.model.Role;

/** Returned after a successful login. The frontend stores the token and sends it on every request. */
public record LoginResponse(String token, Long id, String name, String email, Role role, String dashboard) {
}
