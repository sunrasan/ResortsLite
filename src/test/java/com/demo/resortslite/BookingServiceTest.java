package com.demo.resortslite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookingService}.
 * Covers createBooking, getBookingById, calculateRoomPrice,
 * isRoomAvailable, and generateReport methods.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BookingService bookingService;

    // -----------------------------------------------------------------------
    // createBooking
    // -----------------------------------------------------------------------

    @Test
    void createBooking_withValidParams_returnsMapWithBookingId() {
        // Arrange
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        // Act
        Map<String, Object> result = bookingService.createBooking(
                "Alice", "SUITE", "2024-06-01", "2024-06-05");

        // Assert
        assertNotNull(result);
        assertNotNull(result.get("bookingId"));
        assertTrue(result.get("bookingId").toString().startsWith("BK-"));
    }

    @Test
    void createBooking_withValidParams_returnsGuestName() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        Map<String, Object> result = bookingService.createBooking(
                "Bob", "DELUXE", "2024-07-10", "2024-07-15");

        assertEquals("Bob", result.get("guestName"));
    }

    @Test
    void createBooking_withValidParams_returnsRoomType() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        Map<String, Object> result = bookingService.createBooking(
                "Carol", "VILLA", "2024-08-01", "2024-08-07");

        assertEquals("VILLA", result.get("roomType"));
    }

    @Test
    void createBooking_withValidParams_returnsCheckInAndCheckOut() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        Map<String, Object> result = bookingService.createBooking(
                "Dave", "STANDARD", "2024-09-01", "2024-09-03");

        assertEquals("2024-09-01", result.get("checkIn"));
        assertEquals("2024-09-03", result.get("checkOut"));
    }

    @Test
    void createBooking_withValidParams_returnsConfirmationCode() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        Map<String, Object> result = bookingService.createBooking(
                "Eve", "SUITE", "2024-10-01", "2024-10-04");

        assertNotNull(result.get("confirmationCode"));
        assertFalse(result.get("confirmationCode").toString().isEmpty());
    }

    @Test
    void createBooking_invokesJdbcTemplateUpdate() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        bookingService.createBooking("Frank", "DELUXE", "2024-11-01", "2024-11-03");

        verify(jdbcTemplate, times(1)).update(
                contains("INSERT INTO bookings"),
                any(), any(), any(), any(), any());
    }

    @Test
    void createBooking_confirmationCodeIsSha256Hex() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        Map<String, Object> result = bookingService.createBooking(
                "Grace", "STANDARD", "2024-12-01", "2024-12-02");

        String code = (String) result.get("confirmationCode");
        // SHA-256 hex is always 64 characters
        assertEquals(64, code.length());
        assertTrue(code.matches("[0-9a-f]+"));
    }

    // -----------------------------------------------------------------------
    // getBookingById
    // -----------------------------------------------------------------------

    @Test
    void getBookingById_whenFound_returnsBookingMap() {
        Map<String, Object> dbRow = new HashMap<>();
        dbRow.put("id", "BK-ABCD1234");
        dbRow.put("guest", "Henry");
        when(jdbcTemplate.queryForMap(anyString(), eq("BK-ABCD1234"))).thenReturn(dbRow);

        Map<String, Object> result = bookingService.getBookingById("BK-ABCD1234");

        assertNotNull(result);
        assertEquals("Henry", result.get("guest"));
    }

    @Test
    void getBookingById_whenNotFound_returnsErrorMap() {
        when(jdbcTemplate.queryForMap(anyString(), eq("BK-NOTEXIST")))
                .thenThrow(new RuntimeException("No rows found"));

        Map<String, Object> result = bookingService.getBookingById("BK-NOTEXIST");

        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        assertTrue(result.get("error").toString().contains("BK-NOTEXIST"));
    }

    @Test
    void getBookingById_whenExceptionThrown_errorMessageContainsBookingId() {
        when(jdbcTemplate.queryForMap(anyString(), eq("BK-XYZ")))
                .thenThrow(new RuntimeException("EmptyResultDataAccessException"));

        Map<String, Object> result = bookingService.getBookingById("BK-XYZ");

        assertTrue(result.get("error").toString().contains("BK-XYZ"));
    }

    // -----------------------------------------------------------------------
    // calculateRoomPrice — room type variations
    // -----------------------------------------------------------------------

    @Test
    void calculateRoomPrice_standardRoom_standardSeason_noLoyalty_1Night() {
        String price = bookingService.calculateRoomPrice("STANDARD", 1, "STANDARD", "NONE");
        assertEquals("120.00", price);
    }

    @Test
    void calculateRoomPrice_deluxeRoom_standardSeason_noLoyalty_1Night() {
        String price = bookingService.calculateRoomPrice("DELUXE", 1, "STANDARD", "NONE");
        assertEquals("200.00", price);
    }

    @Test
    void calculateRoomPrice_suiteRoom_standardSeason_noLoyalty_1Night() {
        String price = bookingService.calculateRoomPrice("SUITE", 1, "STANDARD", "NONE");
        assertEquals("350.00", price);
    }

    @Test
    void calculateRoomPrice_villaRoom_standardSeason_noLoyalty_1Night() {
        String price = bookingService.calculateRoomPrice("VILLA", 1, "STANDARD", "NONE");
        assertEquals("600.00", price);
    }

    @Test
    void calculateRoomPrice_unknownRoomType_defaultsToStandardPrice() {
        String price = bookingService.calculateRoomPrice("UNKNOWN", 1, "STANDARD", "NONE");
        assertEquals("120.00", price);
    }

    // -----------------------------------------------------------------------
    // calculateRoomPrice — season variations
    // -----------------------------------------------------------------------

    @Test
    void calculateRoomPrice_peakSeason_appliesMultiplier1_5() {
        // STANDARD base=120, PEAK=120*1.5=180, 1 night, no loyalty
        String price = bookingService.calculateRoomPrice("STANDARD", 1, "PEAK", "NONE");
        assertEquals("180.00", price);
    }

    @Test
    void calculateRoomPrice_offSeason_appliesMultiplier0_8() {
        // STANDARD base=120, OFF=120*0.8=96, 1 night, no loyalty
        String price = bookingService.calculateRoomPrice("STANDARD", 1, "OFF", "NONE");
        assertEquals("96.00", price);
    }

    @Test
    void calculateRoomPrice_defaultSeason_noMultiplier() {
        String price = bookingService.calculateRoomPrice("STANDARD", 1, "REGULAR", "NONE");
        assertEquals("120.00", price);
    }

    // -----------------------------------------------------------------------
    // calculateRoomPrice — loyalty tier variations
    // -----------------------------------------------------------------------

    @Test
    void calculateRoomPrice_goldLoyalty_applies10PercentDiscount() {
        // STANDARD base=120, GOLD=120*0.9=108, 1 night
        String price = bookingService.calculateRoomPrice("STANDARD", 1, "STANDARD", "GOLD");
        assertEquals("108.00", price);
    }

    @Test
    void calculateRoomPrice_platinumLoyalty_applies20PercentDiscount() {
        // STANDARD base=120, PLATINUM=120*0.8=96, 1 night
        String price = bookingService.calculateRoomPrice("STANDARD", 1, "STANDARD", "PLATINUM");
        assertEquals("96.00", price);
    }

    @Test
    void calculateRoomPrice_diamondLoyalty_applies30PercentDiscount() {
        // STANDARD base=120, DIAMOND=120*0.7=84, 1 night
        String price = bookingService.calculateRoomPrice("STANDARD", 1, "STANDARD", "DIAMOND");
        assertEquals("84.00", price);
    }

    @Test
    void calculateRoomPrice_noLoyalty_noDiscount() {
        String price = bookingService.calculateRoomPrice("STANDARD", 1, "STANDARD", "NONE");
        assertEquals("120.00", price);
    }

    // -----------------------------------------------------------------------
    // calculateRoomPrice — night-stay discount thresholds
    // -----------------------------------------------------------------------

    @Test
    void calculateRoomPrice_7NightsOrMore_applies5PercentDiscount() {
        // STANDARD base=120, 7 nights, 5% discount: 120*0.95*7=798.00
        String price = bookingService.calculateRoomPrice("STANDARD", 7, "STANDARD", "NONE");
        assertEquals("798.00", price);
    }

    @Test
    void calculateRoomPrice_14NightsOrMore_applies10PercentDiscount() {
        // STANDARD base=120, 14 nights, 10% discount: 120*0.90*14=1512.00
        String price = bookingService.calculateRoomPrice("STANDARD", 14, "STANDARD", "NONE");
        assertEquals("1512.00", price);
    }

    @Test
    void calculateRoomPrice_6Nights_noStayDiscount() {
        // STANDARD base=120, 6 nights, no stay discount: 120*6=720.00
        String price = bookingService.calculateRoomPrice("STANDARD", 6, "STANDARD", "NONE");
        assertEquals("720.00", price);
    }

    @Test
    void calculateRoomPrice_combinedPeakSeasonDiamondLoyalty14Nights() {
        // SUITE base=350, PEAK=350*1.5=525, DIAMOND=525*0.7=367.5, 14nights=367.5*0.9=330.75, total=330.75*14=4630.50
        String price = bookingService.calculateRoomPrice("SUITE", 14, "PEAK", "DIAMOND");
        assertEquals("4630.50", price);
    }

    @Test
    void calculateRoomPrice_combinedOffSeasonGoldLoyalty7Nights() {
        // DELUXE base=200, OFF=200*0.8=160, GOLD=160*0.9=144, 7nights=144*0.95=136.8, total=136.8*7=957.60
        String price = bookingService.calculateRoomPrice("DELUXE", 7, "OFF", "GOLD");
        assertEquals("957.60", price);
    }

    // -----------------------------------------------------------------------
    // isRoomAvailable
    // -----------------------------------------------------------------------

    @Test
    void isRoomAvailable_standardRoom_returnsTrue() {
        assertTrue(bookingService.isRoomAvailable("STANDARD"));
    }

    @Test
    void isRoomAvailable_deluxeRoom_returnsTrue() {
        assertTrue(bookingService.isRoomAvailable("DELUXE"));
    }

    @Test
    void isRoomAvailable_suiteRoom_returnsTrue() {
        assertTrue(bookingService.isRoomAvailable("SUITE"));
    }

    @Test
    void isRoomAvailable_villaRoom_returnsTrue() {
        assertTrue(bookingService.isRoomAvailable("VILLA"));
    }

    @Test
    void isRoomAvailable_unknownRoomType_returnsFalse() {
        assertFalse(bookingService.isRoomAvailable("PENTHOUSE"));
    }

    @Test
    void isRoomAvailable_nullRoomType_returnsFalse() {
        assertFalse(bookingService.isRoomAvailable(null));
    }

    @Test
    void isRoomAvailable_emptyString_returnsFalse() {
        assertFalse(bookingService.isRoomAvailable(""));
    }

    @Test
    void isRoomAvailable_lowercaseRoomType_returnsFalse() {
        assertFalse(bookingService.isRoomAvailable("standard"));
    }

    // -----------------------------------------------------------------------
    // generateReport
    // -----------------------------------------------------------------------

    @Test
    void generateReport_returnsStringContainingMonth() {
        String result = bookingService.generateReport("March");
        assertNotNull(result);
        assertTrue(result.contains("March"));
    }

    @Test
    void generateReport_returnsNonEmptyString() {
        String result = bookingService.generateReport("January");
        assertFalse(result.isEmpty());
    }

    @Test
    void generateReport_returnsStringContainingTriggeredKeyword() {
        String result = bookingService.generateReport("July");
        assertTrue(result.toLowerCase().contains("report"));
    }
}
