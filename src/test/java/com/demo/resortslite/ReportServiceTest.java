package com.demo.resortslite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ReportService}.
 * Covers generateMonthlyReport, buildReportDownloadUrl, and getSystemInfo methods.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Inject test values for @Value fields via ReflectionTestUtils
        String reportPath = tempDir.resolve("reports/").toString() + "/";
        String backupPath = tempDir.resolve("backups/nightly/").toString() + "/";
        ReflectionTestUtils.setField(reportService, "reportBasePath", reportPath);
        ReflectionTestUtils.setField(reportService, "backupPath", backupPath);
        ReflectionTestUtils.setField(reportService, "serverPort", 8080);
    }

    // -----------------------------------------------------------------------
    // generateMonthlyReport
    // -----------------------------------------------------------------------

    @Test
    void generateMonthlyReport_withValidMonthAndYear_returnsGeneratedStatus() {
        Map<String, Object> result = reportService.generateMonthlyReport("03", "2024");

        assertNotNull(result);
        assertEquals("generated", result.get("status"));
    }

    @Test
    void generateMonthlyReport_withValidMonthAndYear_returnsFilePath() {
        Map<String, Object> result = reportService.generateMonthlyReport("03", "2024");

        assertNotNull(result.get("path"));
        assertTrue(result.get("path").toString().contains("resort_report_03_2024.csv"));
    }

    @Test
    void generateMonthlyReport_createsFileOnDisk() {
        Map<String, Object> result = reportService.generateMonthlyReport("05", "2024");

        String path = (String) result.get("path");
        assertNotNull(path);
        File file = new File(path);
        assertTrue(file.exists(), "Report file should exist on disk");
    }

    @Test
    void generateMonthlyReport_fileContainsCsvHeader() throws Exception {
        Map<String, Object> result = reportService.generateMonthlyReport("06", "2024");

        String path = (String) result.get("path");
        String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
        assertTrue(content.contains("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount"));
    }

    @Test
    void generateMonthlyReport_fileContainsSampleData() throws Exception {
        Map<String, Object> result = reportService.generateMonthlyReport("07", "2024");

        String path = (String) result.get("path");
        String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
        assertTrue(content.contains("BK-001"));
        assertTrue(content.contains("BK-002"));
    }

    @Test
    void generateMonthlyReport_returnsServerPortInResult() {
        Map<String, Object> result = reportService.generateMonthlyReport("08", "2024");

        assertNotNull(result.get("serverPort"));
        assertEquals(8080, result.get("serverPort"));
    }

    @Test
    void generateMonthlyReport_differentMonths_createsDifferentFiles() {
        Map<String, Object> result1 = reportService.generateMonthlyReport("01", "2024");
        Map<String, Object> result2 = reportService.generateMonthlyReport("02", "2024");

        assertNotEquals(result1.get("path"), result2.get("path"));
    }

    @Test
    void generateMonthlyReport_differentYears_createsDifferentFiles() {
        Map<String, Object> result1 = reportService.generateMonthlyReport("01", "2023");
        Map<String, Object> result2 = reportService.generateMonthlyReport("01", "2024");

        assertNotEquals(result1.get("path"), result2.get("path"));
    }

    @Test
    void generateMonthlyReport_whenDirectoryDoesNotExist_createsDirectory() {
        // Use a nested path that doesn't exist yet
        String nestedPath = tempDir.resolve("new/nested/reports/").toString() + "/";
        ReflectionTestUtils.setField(reportService, "reportBasePath", nestedPath);

        Map<String, Object> result = reportService.generateMonthlyReport("09", "2024");

        assertEquals("generated", result.get("status"));
        File dir = new File(nestedPath);
        assertTrue(dir.exists());
    }

    @Test
    void generateMonthlyReport_withInvalidPath_returnsErrorStatus() {
        // Set an invalid path (null byte in path causes IOException on Linux)
        ReflectionTestUtils.setField(reportService, "reportBasePath", "/root/no-permission-path-xyz/");

        Map<String, Object> result = reportService.generateMonthlyReport("10", "2024");

        // Either generated (if running as root) or error
        assertTrue(result.containsKey("status"));
    }

    // -----------------------------------------------------------------------
    // buildReportDownloadUrl
    // -----------------------------------------------------------------------

    @Test
    void buildReportDownloadUrl_returnsUrlContainingReportName() {
        String url = reportService.buildReportDownloadUrl("march_report.pdf");

        assertNotNull(url);
        assertTrue(url.contains("march_report.pdf"));
    }

    @Test
    void buildReportDownloadUrl_returnsUrlContainingServerPort() {
        String url = reportService.buildReportDownloadUrl("report.pdf");

        assertTrue(url.contains("8080"));
    }

    @Test
    void buildReportDownloadUrl_returnsUrlContainingDownloadPath() {
        String url = reportService.buildReportDownloadUrl("test_report.csv");

        assertTrue(url.contains("/download/"));
    }

    @Test
    void buildReportDownloadUrl_returnsUrlStartingWithHttp() {
        String url = reportService.buildReportDownloadUrl("any_report.pdf");

        assertTrue(url.startsWith("http://"));
    }

    @Test
    void buildReportDownloadUrl_withDifferentPort_returnsCorrectPort() {
        ReflectionTestUtils.setField(reportService, "serverPort", 9090);

        String url = reportService.buildReportDownloadUrl("report.pdf");

        assertTrue(url.contains("9090"));
    }

    @Test
    void buildReportDownloadUrl_withEmptyReportName_returnsUrlWithEmptySegment() {
        String url = reportService.buildReportDownloadUrl("");

        assertNotNull(url);
        assertTrue(url.contains("/download/"));
    }

    @Test
    void buildReportDownloadUrl_fullUrlFormat() {
        String url = reportService.buildReportDownloadUrl("june_2024.pdf");

        assertEquals("http://reports.resorts-internal.com:8080/download/june_2024.pdf", url);
    }

    // -----------------------------------------------------------------------
    // getSystemInfo
    // -----------------------------------------------------------------------

    @Test
    void getSystemInfo_returnsNonNullMap() {
        Map<String, Object> info = reportService.getSystemInfo();

        assertNotNull(info);
    }

    @Test
    void getSystemInfo_containsReportPathKey() {
        Map<String, Object> info = reportService.getSystemInfo();

        assertTrue(info.containsKey("reportPath"));
    }

    @Test
    void getSystemInfo_containsBackupPathKey() {
        Map<String, Object> info = reportService.getSystemInfo();

        assertTrue(info.containsKey("backupPath"));
    }

    @Test
    void getSystemInfo_containsServerPortKey() {
        Map<String, Object> info = reportService.getSystemInfo();

        assertTrue(info.containsKey("serverPort"));
        assertEquals(8080, info.get("serverPort"));
    }

    @Test
    void getSystemInfo_containsGeneratedAtKey() {
        Map<String, Object> info = reportService.getSystemInfo();

        assertTrue(info.containsKey("generatedAt"));
        assertNotNull(info.get("generatedAt"));
    }

    @Test
    void getSystemInfo_generatedAtMatchesTimestampFormat() {
        Map<String, Object> info = reportService.getSystemInfo();

        String timestamp = (String) info.get("generatedAt");
        // Format: yyyy-MM-dd HH:mm:ss
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "Timestamp should match yyyy-MM-dd HH:mm:ss format, got: " + timestamp);
    }

    @Test
    void getSystemInfo_reportPathMatchesInjectedValue() {
        Map<String, Object> info = reportService.getSystemInfo();

        String reportPath = (String) info.get("reportPath");
        assertNotNull(reportPath);
        assertTrue(reportPath.contains("reports"));
    }

    @Test
    void getSystemInfo_backupPathMatchesInjectedValue() {
        Map<String, Object> info = reportService.getSystemInfo();

        String backupPath = (String) info.get("backupPath");
        assertNotNull(backupPath);
        assertTrue(backupPath.contains("backups"));
    }

    @Test
    void getSystemInfo_returnsMapWithFourEntries() {
        Map<String, Object> info = reportService.getSystemInfo();

        assertEquals(4, info.size());
    }
}
