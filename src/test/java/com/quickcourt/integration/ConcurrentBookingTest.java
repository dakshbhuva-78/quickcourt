package com.quickcourt.integration;

import com.jayway.jsonpath.JsonPath;
import com.quickcourt.model.*;
import com.quickcourt.repository.CourtRepository;
import com.quickcourt.repository.UserRepository;
import com.quickcourt.repository.VenueRepository;
import com.quickcourt.util.TimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * CONCURRENCY TEST: 10 different players try to book the SAME slot at the same moment.
 * Exactly ONE booking must succeed (HTTP 201); the other nine must get HTTP 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcurrentBookingTest {

    private static final String PASSWORD = "Strong@123";
    private static final int PLAYERS = 10;

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired VenueRepository venueRepository;
    @Autowired CourtRepository courtRepository;
    @Autowired PasswordEncoder encoder;

    private String loginToken(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    void onlyOnePlayerGetsTheSlot() throws Exception {
        // arrange: one approved venue with one court
        VenueOwner owner = userRepository.save(new VenueOwner("Owner", "owner-" + UUID.randomUUID() + "@test.com", encoder.encode(PASSWORD)));
        Venue venue = new Venue();
        venue.setOwner(owner);
        venue.setName("Concurrent Venue");
        venue.setLocation("Test City");
        venue.setSportType(SportType.BADMINTON);
        venue.setPricePerHour(new BigDecimal("300"));
        venue.setOpeningTime(LocalTime.of(6, 0));
        venue.setClosingTime(LocalTime.of(22, 0));
        venue.setStatus(VenueStatus.APPROVED);
        venue = venueRepository.save(venue);
        Court court = new Court();
        court.setVenue(venue);
        court.setCourtName("Court 1");
        court.setSportType(SportType.BADMINTON);
        court = courtRepository.save(court);

        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < PLAYERS; i++) {
            String email = "racer" + i + "-" + UUID.randomUUID() + "@test.com";
            userRepository.save(new Player("Racer " + i, email, encoder.encode(PASSWORD)));
            tokens.add(loginToken(email));
        }

        String json = String.format("{\"venueId\":%d,\"courtId\":%d,\"bookingDate\":\"%s\",\"timeSlot\":\"18:00-19:00\"}",
                venue.getId(), court.getId(), TimeUtil.today().plusDays(5));

        // act: all players fire at the same moment
        ExecutorService pool = Executors.newFixedThreadPool(PLAYERS);
        CountDownLatch ready = new CountDownLatch(PLAYERS);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (String token : tokens) {
            futures.add(pool.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    int status = mvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON).content(json))
                            .andReturn().getResponse().getStatus();
                    if (status == 201) created.incrementAndGet();
                    if (status == 409) conflicts.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        ready.await();
        go.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        // assert
        assertEquals(1, created.get(), "exactly one booking must succeed");
        assertEquals(PLAYERS - 1, conflicts.get(), "all other players must receive 409");
    }
}
