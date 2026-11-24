package com.quickcourt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** A facility owner who lists venues and courts. */
@Entity
@DiscriminatorValue("VENUE_OWNER")
public class VenueOwner extends User {

    public VenueOwner() {
        setRole(Role.VENUE_OWNER);
    }

    public VenueOwner(String name, String email, String passwordHash) {
        this();
        setName(name);
        setEmail(email);
        setPassword(passwordHash);
    }

    @Override
    @JsonIgnore
    public String getDashboardPath() {
        return "owner-dashboard.html";
    }
}
