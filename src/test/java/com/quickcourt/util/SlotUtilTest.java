package com.quickcourt.util;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for SlotUtil (boundary value analysis on opening hours). */
class SlotUtilTest {

    @Test
    void generatesOneSlotPerHour() {
        List<String> slots = SlotUtil.generateSlots(LocalTime.of(6, 0), LocalTime.of(9, 0));
        assertEquals(List.of("06:00-07:00", "07:00-08:00", "08:00-09:00"), slots);
    }

    @Test
    void exactlyOneHourGivesOneSlot() {
        assertEquals(1, SlotUtil.generateSlots(LocalTime.of(10, 0), LocalTime.of(11, 0)).size());
    }

    @Test
    void lessThanOneHourGivesNoSlot() {
        assertTrue(SlotUtil.generateSlots(LocalTime.of(10, 0), LocalTime.of(10, 30)).isEmpty());
    }

    @Test
    void closingBeforeOpeningGivesNoSlot() {
        assertTrue(SlotUtil.generateSlots(LocalTime.of(22, 0), LocalTime.of(6, 0)).isEmpty());
    }

    @Test
    void partialLastHourIsDropped() {
        assertEquals(1, SlotUtil.generateSlots(LocalTime.of(6, 0), LocalTime.of(7, 30)).size());
    }

    @Test
    void nullTimesGiveEmptyList() {
        assertTrue(SlotUtil.generateSlots(null, LocalTime.of(9, 0)).isEmpty());
    }

    @Test
    void validatesSlotFormat() {
        assertTrue(SlotUtil.isValidFormat("18:00-19:00"));
        assertFalse(SlotUtil.isValidFormat("18:00"));
        assertFalse(SlotUtil.isValidFormat("6:00-7:00"));
        assertFalse(SlotUtil.isValidFormat(null));
    }

    @Test
    void parsesStartAndEnd() {
        assertEquals(LocalTime.of(18, 0), SlotUtil.startOf("18:00-19:00"));
        assertEquals(LocalTime.of(19, 0), SlotUtil.endOf("18:00-19:00"));
    }
}
