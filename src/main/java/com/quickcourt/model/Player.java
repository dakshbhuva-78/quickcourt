package com.quickcourt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** A normal user who searches and books courts. */
@Entity
@DiscriminatorValue("PLAYER")
public class Player extends User {

    public Player() {
        setRole(Role.PLAYER);
    }

    public Player(String name, String email, String passwordHash) {
        this();
        setName(name);
        setEmail(email);
        setPassword(passwordHash);
    }

    @Override
    @JsonIgnore
    public String getDashboardPath() {
        return "venues.html";
    }
}
