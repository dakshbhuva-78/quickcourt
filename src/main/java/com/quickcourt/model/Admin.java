package com.quickcourt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Platform administrator (created by the seeder, cannot self-register). */
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    public Admin() {
        setRole(Role.ADMIN);
    }

    public Admin(String name, String email, String passwordHash) {
        this();
        setName(name);
        setEmail(email);
        setPassword(passwordHash);
    }

    @Override
    @JsonIgnore
    public String getDashboardPath() {
        return "admin-dashboard.html";
    }
}
