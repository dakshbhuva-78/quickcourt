package com.quickcourt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

/** One playable court / turf / table inside a Venue. */
@Entity
@Table(name = "courts")
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String courtName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SportType sportType;

    /** false = court removed by the owner (soft delete, old bookings stay valid). */
    private boolean active = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "venue_id")
    @JsonIgnoreProperties({"owner"})
    private Venue venue;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public SportType getSportType() { return sportType; }
    public void setSportType(SportType sportType) { this.sportType = sportType; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Venue getVenue() { return venue; }
    public void setVenue(Venue venue) { this.venue = venue; }
}
