package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Issue fix: Legacy java.util.Date and SimpleDateFormat Used in ReportService
// Replaced legacy java.util.Date / SimpleDateFormat with java.time API.
// java.time.LocalDateTime and DateTimeFormatter are the modern replacements (Java 8+).
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // Issue fix: Hardcoded absolute path /var/legacy/reports removed.
    // Report base path is now externalised to environment variable REPORT_BASE_PATH.
    // Default falls back to a relative temp directory safe for containerised deployments.
    @Value("${app.report.base-path:${java.io.tmpdir}/resort-reports/}")
    private String reportBasePath;

    // Issue fix: Windows-style absolute path C:\ResortBackups\nightly\ removed.
    // Backup path is now externalised to environment variable BACKUP_PATH.
    @Value("${app.report.backup-path:${java.io.tmpdir}/resort-backups/}")
    private String backupPath;

    // Issue fix: Fixed server port hardcoded in application logic removed.
    // Server port is now read from Spring environment (server.port property).
    @Value("${server.port:8080}")
    private int serverPort;

    // Issue fix: Plain HTTP report download URL replaced with HTTPS and externalised.
    // Report download base URL is now configurable via environment variable.
    @Value("${app.report.download-base-url:https://reports.resorts-internal.com/download}")
    private String reportDownloadBaseUrl;

    /**
     * Generates a monthly booking report as a CSV file.
     *
     * @param month the month for the report (e.g., "March")
     * @param year  the year for the report (e.g., "2024")
     * @return a map containing the report generation status and file path
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String fullPath = reportBasePath + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            java.io.File reportDir = new java.io.File(reportBasePath);
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }

            java.io.FileWriter writer = new java.io.FileWriter(fullPath);
            writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            writer.close();

            result.put("status", "generated");
            result.put("path", fullPath);
            // Issue fix: serverPort no longer hardcoded — injected via @Value
            result.put("serverPort", serverPort);

        } catch (java.io.IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds the download URL for a given report name.
     * Issue fix: Plain HTTP URL replaced with HTTPS; base URL externalised to config.
     *
     * @param reportName the name of the report file
     * @return the full download URL for the report
     */
    public String buildReportDownloadUrl(String reportName) {
        return reportDownloadBaseUrl + "/" + reportName;
    }

    /**
     * Returns system information including report paths, backup path, server port,
     * and the current timestamp formatted using the modern java.time API.
     *
     * @return a map containing system information key-value pairs
     */
    public Map<String, Object> getSystemInfo() {
        // Issue fix: Legacy java.util.Date and SimpleDateFormat Used in ReportService
        // Replaced: new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        // With: java.time.LocalDateTime + DateTimeFormatter (thread-safe, modern Java API)
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Map<String, Object> info = new HashMap<>();
        // Issue fix: Paths and port are now injected via @Value — no hardcoded values
        info.put("reportPath", reportBasePath);
        info.put("backupPath", backupPath);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        return info;
    }
}
