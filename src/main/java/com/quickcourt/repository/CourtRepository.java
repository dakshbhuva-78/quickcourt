package com.quickcourt.repository;

import com.quickcourt.model.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourtRepository extends JpaRepository<Court, Long> {

    @Query("select c from Court c where c.venue.id = :venueId order by c.id")
    List<Court> findByVenueId(@Param("venueId") Long venueId);

    @Query("select c from Court c where c.venue.id = :venueId and c.active = true order by c.id")
    List<Court> findActiveByVenueId(@Param("venueId") Long venueId);

    long countByActiveTrue();

    @Query("select count(c) from Court c where c.venue.owner.id = :ownerId and c.active = true")
    long countActiveByOwner(@Param("ownerId") Long ownerId);
}
