package com.quickcourt.repository;

import com.quickcourt.model.BlockedSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BlockedSlotRepository extends JpaRepository<BlockedSlot, Long> {

    @Query("select count(s) from BlockedSlot s where s.court.id = :courtId and s.blockDate = :date and s.timeSlot = :slot")
    long countBlocked(@Param("courtId") Long courtId, @Param("date") LocalDate date, @Param("slot") String slot);

    default boolean existsBlocked(Long courtId, LocalDate date, String slot) {
        return countBlocked(courtId, date, slot) > 0;
    }

    @Query("select s from BlockedSlot s where s.court.id = :courtId and s.blockDate = :date")
    List<BlockedSlot> findForCourtAndDate(@Param("courtId") Long courtId, @Param("date") LocalDate date);

    @Query("select s from BlockedSlot s where s.court.id = :courtId and s.blockDate >= :fromDate order by s.blockDate, s.timeSlot")
    List<BlockedSlot> findUpcomingForCourt(@Param("courtId") Long courtId, @Param("fromDate") LocalDate fromDate);
}
