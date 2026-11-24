package com.quickcourt.repository;

import com.quickcourt.model.Venue;
import com.quickcourt.model.VenueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findByStatus(VenueStatus status);

    @Query("select v from Venue v where v.owner.id = :ownerId order by v.id desc")
    List<Venue> findByOwnerId(@Param("ownerId") Long ownerId);
}
