package com.quickcourt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDate;

/** A slot the owner blocked (maintenance, private event ...). Players cannot book it. */
@Entity
@Table(name = "blocked_slots")
public class BlockedSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "court_id")
    @JsonIgnoreProperties({"venue"})
    private Court court;

    @Column(nullable = false)
    private LocalDate blockDate;

    @Column(nullable = false, length = 11)
    private String timeSlot;

    private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Court getCourt() { return court; }
    public void setCourt(Court court) { this.court = court; }

    public LocalDate getBlockDate() { return blockDate; }
    public void setBlockDate(LocalDate blockDate) { this.blockDate = blockDate; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
