package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ResortsLiteApplication}.
 * Verifies that the Spring application context loads successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
class ResortsLiteApplicationTest {

    /**
     * Verifies that the Spring application context loads without errors.
     * This is the standard smoke test for Spring Boot applications.
     */
    @Test
    void contextLoads() {
        // If the context fails to load, this test will fail automatically
        // No explicit assertion needed — Spring Boot test infrastructure handles it
        assertTrue(true, "Application context should load successfully");
    }

    /**
     * Verifies that the main method can be invoked without throwing exceptions.
     * Sets the "test" Spring profile so H2 in-memory database is used instead of PostgreSQL.
     */
    @Test
    void main_doesNotThrowException() {
        // Set the "test" profile so the application uses H2 (not PostgreSQL) when main() is called
        System.setProperty("spring.profiles.active", "test");
        try {
            assertDoesNotThrow(() ->
                    ResortsLiteApplication.main(new String[]{"--spring.profiles.active=test",
                            "--spring.main.web-application-type=none"}),
                    "main() should not throw any exception"
            );
        } finally {
            System.clearProperty("spring.profiles.active");
        }
    }
}
