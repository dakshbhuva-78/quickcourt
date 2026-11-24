package com.quickcourt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalTime;

/** A sports facility (e.g. "Green Arena Badminton Club"). It owns several Courts. */
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SportType sportType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    private LocalTime openingTime;
    private LocalTime closingTime;

    /** Comma separated, e.g. "Parking,Washroom,Drinking Water". */
    @Column(length = 500)
    private String amenities;

    /** Optional http(s) link to a photo. If empty the website shows a sport illustration. */
    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VenueStatus status = VenueStatus.PENDING;

    /** Optional note written by the admin when approving/rejecting. */
    @Column(length = 500)
    private String adminComment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    @JsonIgnoreProperties({"email", "role", "active", "createdAt"})
    private User owner;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public SportType getSportType() { return sportType; }
    public void setSportType(SportType sportType) { this.sportType = sportType; }

    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }

    public LocalTime getOpeningTime() { return openingTime; }
    public void setOpeningTime(LocalTime openingTime) { this.openingTime = openingTime; }

    public LocalTime getClosingTime() { return closingTime; }
    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public VenueStatus getStatus() { return status; }
    public void setStatus(VenueStatus status) { this.status = status; }

    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
}
