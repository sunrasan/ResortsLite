package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
// Migrated from legacy java.util.Date + java.text.SimpleDateFormat to java.time API
// for Java 17 compatibility
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // Externalised to environment variable — avoids hardcoded absolute path.
    // For cloud deployments, replace with AWS S3 or equivalent object storage.
    @Value("${app.report.base-path:${java.io.tmpdir}/reports/}")
    private String reportBasePath;

    // Externalised to environment variable — replaces Windows-style hardcoded path
    // that fails on Linux-based containers.
    @Value("${app.report.backup-path:${java.io.tmpdir}/backups/nightly/}")
    private String backupPath;

    // Externalised to environment variable — container orchestration (ECS/EKS)
    // dynamically assigns ports; do not hardcode in application logic.
    @Value("${SERVER_PORT:8080}")
    private int serverPort;

    /**
     * Generates a monthly booking report CSV file.
     *
     * @param month the month (e.g., "03")
     * @param year  the year (e.g., "2024")
     * @return a map containing the generation status and file path
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String fullPath = reportBasePath + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            File reportDir = new File(reportBasePath);
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(fullPath)) {
                writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
                writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
                writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            }

            result.put("status", "generated");
            result.put("path", fullPath);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds the download URL for a given report name.
     * NOTE: Plain HTTP URL — enforce HTTPS for cloud/production deployments.
     *
     * @param reportName the name of the report file
     * @return the download URL string
     */
    public String buildReportDownloadUrl(String reportName) {
        return "http://reports.resorts-internal.com:" + serverPort + "/download/" + reportName;
    }

    /**
     * Returns system information including report paths and current timestamp.
     *
     * @return a map of system info key-value pairs
     */
    public Map<String, Object> getSystemInfo() {
        // Using java.time.LocalDateTime (Java 17 compatible) instead of legacy java.util.Date
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Map<String, Object> info = new HashMap<>();
        info.put("reportPath", reportBasePath);
        info.put("backupPath", backupPath);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        return info;
    }
}
