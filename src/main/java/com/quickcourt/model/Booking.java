package com.quickcourt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One booked hour on one court. */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"email", "role", "active", "createdAt"})
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "venue_id")
    @JsonIgnoreProperties({"owner"})
    private Venue venue;

    @ManyToOne(optional = false)
    @JoinColumn(name = "court_id")
    @JsonIgnoreProperties({"venue"})
    private Court court;

    @Column(nullable = false)
    private LocalDate bookingDate;

    /** Format "HH:mm-HH:mm", e.g. "18:00-19:00". */
    @Column(nullable = false, length = 11)
    private String timeSlot;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Venue getVenue() { return venue; }
    public void setVenue(Venue venue) { this.venue = venue; }

    public Court getCourt() { return court; }
    public void setCourt(Court court) { this.court = court; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BookingStatus getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(BookingStatus bookingStatus) { this.bookingStatus = bookingStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
