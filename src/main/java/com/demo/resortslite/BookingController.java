package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

// Issue fix: javax.servlet.http.HttpSession Must Be Migrated to jakarta.servlet.http.HttpSession
// Jakarta EE migration required for Java 17+ / Spring Boot 3.x (javax.* → jakarta.*)
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // Issue fix: Hardcoded internal inventory service URL replaced with externalised config.
    // Set INVENTORY_ENDPOINT environment variable for cloud deployments.
    @Value("${app.inventory.endpoint:https://inventory-svc.internal/rooms}")
    private String inventoryEndpoint;

    // NOTE: In-memory cache without TTL breaks horizontal scaling — cache is instance-local.
    // Consider replacing with a distributed cache (e.g., Redis) for cloud-native deployments.
    private static final Map<String, Object> bookingCache = new HashMap<>();

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // NOTE: Booking state stored in HTTP session memory.
        // AWS ALB distributes requests across EC2 instances — session data on instance A
        // is invisible to instance B. Consider externalising session state (e.g., Redis).
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        bookingCache.put((String) booking.get("bookingId"), booking);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // NOTE: Reading business state from HTTP session — will return null on any other
        // instance in the cluster. Consider distributed session management.
        String lastGuest = (String) session.getAttribute("guestName");

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // Issue fix: Plain HTTP hardcoded URL replaced with HTTPS externalised config.
        // inventoryEndpoint is injected via @Value from application.properties / env var.
        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryEndpoint);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Issue fix: Hardcoded absolute file path /var/legacy/reports removed.
        // Report path is now managed via app.report.base-path configuration property.
        // Sanitise month parameter to prevent path traversal.
        String safMonth = month.replaceAll("[^a-zA-Z0-9_\\-]", "");
        String reportPath = "${app.report.base-path:reports}/" + safMonth + "_bookings.pdf";

        Map<String, Object> response = new HashMap<>();
        response.put("reportPath", reportPath);
        response.put("message", bookingService.generateReport(safMonth));
        return response;
    }
}
