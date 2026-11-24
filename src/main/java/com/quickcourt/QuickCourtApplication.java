package com.quickcourt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of QuickCourt. Run this class (or use "mvn spring-boot:run")
 * to start the embedded web server on http://localhost:8080
 */
@SpringBootApplication
public class QuickCourtApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuickCourtApplication.class, args);
    }
}
