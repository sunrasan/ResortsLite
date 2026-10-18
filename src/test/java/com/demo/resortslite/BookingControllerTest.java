package com.demo.resortslite;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookingController}.
 * Covers createBooking, getBookingStatus, checkAvailability, and downloadReport endpoints.
 */
@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private BookingController bookingController;

    @BeforeEach
    void setUp() {
        // Inject the inventoryEndpoint value via ReflectionTestUtils
        ReflectionTestUtils.setField(bookingController,
                "inventoryEndpoint", "http://inventory-svc.internal:8081/rooms");
    }

    // -----------------------------------------------------------------------
    // createBooking
    // -----------------------------------------------------------------------

    @Test
    void createBooking_withValidParams_returnsConfirmedStatus() {
        // Arrange
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-TESTID1");
        mockBooking.put("guestName", "Alice");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockBooking);

        // Act
        Map<String, Object> response = bookingController.createBooking(
                "Alice", "SUITE", "2024-06-01", "2024-06-05", httpSession);

        // Assert
        assertNotNull(response);
        assertEquals("confirmed", response.get("status"));
    }

    @Test
    void createBooking_withValidParams_returnsBookingInResponse() {
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-TESTID2");
        mockBooking.put("guestName", "Bob");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockBooking);

        Map<String, Object> response = bookingController.createBooking(
                "Bob", "DELUXE", "2024-07-01", "2024-07-03", httpSession);

        assertNotNull(response.get("booking"));
        @SuppressWarnings("unchecked")
        Map<String, Object> booking = (Map<String, Object>) response.get("booking");
        assertEquals("BK-TESTID2", booking.get("bookingId"));
    }

    @Test
    void createBooking_setsSessionAttributes() {
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-SESSION1");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockBooking);

        bookingController.createBooking(
                "Carol", "VILLA", "2024-08-01", "2024-08-07", httpSession);

        verify(httpSession).setAttribute("lastBooking", mockBooking);
        verify(httpSession).setAttribute("guestName", "Carol");
    }

    @Test
    void createBooking_invokesBookingService() {
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-SVC001");
        when(bookingService.createBooking("Dave", "STANDARD", "2024-09-01", "2024-09-02"))
                .thenReturn(mockBooking);

        bookingController.createBooking(
                "Dave", "STANDARD", "2024-09-01", "2024-09-02", httpSession);

        verify(bookingService, times(1))
                .createBooking("Dave", "STANDARD", "2024-09-01", "2024-09-02");
    }

    @Test
    void createBooking_addsBookingToCache() {
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-CACHE01");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockBooking);

        Map<String, Object> response = bookingController.createBooking(
                "Eve", "SUITE", "2024-10-01", "2024-10-04", httpSession);

        // Response should contain the booking
        assertNotNull(response.get("booking"));
    }

    // -----------------------------------------------------------------------
    // getBookingStatus
    // -----------------------------------------------------------------------

    @Test
    void getBookingStatus_returnsBookingIdInResponse() {
        Map<String, Object> mockDetails = new HashMap<>();
        mockDetails.put("id", "BK-STATUS1");
        when(bookingService.getBookingById("BK-STATUS1")).thenReturn(mockDetails);
        when(httpSession.getAttribute("guestName")).thenReturn("Frank");

        Map<String, Object> result = bookingController.getBookingStatus("BK-STATUS1", httpSession);

        assertEquals("BK-STATUS1", result.get("bookingId"));
    }

    @Test
    void getBookingStatus_returnsSessionGuestName() {
        Map<String, Object> mockDetails = new HashMap<>();
        when(bookingService.getBookingById(anyString())).thenReturn(mockDetails);
        when(httpSession.getAttribute("guestName")).thenReturn("Grace");

        Map<String, Object> result = bookingController.getBookingStatus("BK-STATUS2", httpSession);

        assertEquals("Grace", result.get("sessionGuest"));
    }

    @Test
    void getBookingStatus_returnsDetailsFromService() {
        Map<String, Object> mockDetails = new HashMap<>();
        mockDetails.put("guest", "Henry");
        mockDetails.put("room", "SUITE");
        when(bookingService.getBookingById("BK-DETAIL1")).thenReturn(mockDetails);
        when(httpSession.getAttribute("guestName")).thenReturn(null);

        Map<String, Object> result = bookingController.getBookingStatus("BK-DETAIL1", httpSession);

        assertNotNull(result.get("details"));
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) result.get("details");
        assertEquals("Henry", details.get("guest"));
    }

    @Test
    void getBookingStatus_whenSessionGuestIsNull_returnsNullSessionGuest() {
        Map<String, Object> mockDetails = new HashMap<>();
        when(bookingService.getBookingById(anyString())).thenReturn(mockDetails);
        when(httpSession.getAttribute("guestName")).thenReturn(null);

        Map<String, Object> result = bookingController.getBookingStatus("BK-NULL1", httpSession);

        assertNull(result.get("sessionGuest"));
    }

    @Test
    void getBookingStatus_invokesBookingService() {
        Map<String, Object> mockDetails = new HashMap<>();
        when(bookingService.getBookingById("BK-INVOKE1")).thenReturn(mockDetails);
        when(httpSession.getAttribute("guestName")).thenReturn("Ivy");

        bookingController.getBookingStatus("BK-INVOKE1", httpSession);

        verify(bookingService, times(1)).getBookingById("BK-INVOKE1");
    }

    // -----------------------------------------------------------------------
    // checkAvailability
    // -----------------------------------------------------------------------

    @Test
    void checkAvailability_withAvailableRoom_returnsAvailableTrue() {
        when(bookingService.isRoomAvailable("SUITE")).thenReturn(true);

        Map<String, Object> result = bookingController.checkAvailability("SUITE");

        assertTrue((Boolean) result.get("available"));
    }

    @Test
    void checkAvailability_withUnavailableRoom_returnsAvailableFalse() {
        when(bookingService.isRoomAvailable("PENTHOUSE")).thenReturn(false);

        Map<String, Object> result = bookingController.checkAvailability("PENTHOUSE");

        assertFalse((Boolean) result.get("available"));
    }

    @Test
    void checkAvailability_returnsRoomTypeInResponse() {
        when(bookingService.isRoomAvailable("DELUXE")).thenReturn(true);

        Map<String, Object> result = bookingController.checkAvailability("DELUXE");

        assertEquals("DELUXE", result.get("roomType"));
    }

    @Test
    void checkAvailability_returnsInventoryEndpointInResponse() {
        when(bookingService.isRoomAvailable("STANDARD")).thenReturn(true);

        Map<String, Object> result = bookingController.checkAvailability("STANDARD");

        assertNotNull(result.get("inventoryEndpoint"));
        assertTrue(result.get("inventoryEndpoint").toString().contains("/available"));
    }

    @Test
    void checkAvailability_invokesBookingService() {
        when(bookingService.isRoomAvailable("VILLA")).thenReturn(true);

        bookingController.checkAvailability("VILLA");

        verify(bookingService, times(1)).isRoomAvailable("VILLA");
    }

    // -----------------------------------------------------------------------
    // downloadReport
    // -----------------------------------------------------------------------

    @Test
    void downloadReport_returnsReportPathInResponse() {
        when(bookingService.generateReport("March")).thenReturn("Report generated for March");

        Map<String, Object> result = bookingController.downloadReport("March");

        assertNotNull(result.get("reportPath"));
        assertTrue(result.get("reportPath").toString().contains("March"));
    }

    @Test
    void downloadReport_returnsMessageFromService() {
        when(bookingService.generateReport("June")).thenReturn("Report generated for June");

        Map<String, Object> result = bookingController.downloadReport("June");

        assertEquals("Report generated for June", result.get("message"));
    }

    @Test
    void downloadReport_reportPathContainsPdfExtension() {
        when(bookingService.generateReport("December")).thenReturn("Report for December");

        Map<String, Object> result = bookingController.downloadReport("December");

        assertTrue(result.get("reportPath").toString().endsWith("_bookings.pdf"));
    }

    @Test
    void downloadReport_invokesBookingService() {
        when(bookingService.generateReport("January")).thenReturn("Report for January");

        bookingController.downloadReport("January");

        verify(bookingService, times(1)).generateReport("January");
    }

    @Test
    void downloadReport_responseContainsBothKeys() {
        when(bookingService.generateReport("April")).thenReturn("Report for April");

        Map<String, Object> result = bookingController.downloadReport("April");

        assertTrue(result.containsKey("reportPath"));
        assertTrue(result.containsKey("message"));
    }
}
