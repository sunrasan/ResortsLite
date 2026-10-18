package com.demo.resortslite;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
// Issue fix: Legacy java.util.Date and SimpleDateFormat Used in ReportService
// Replaced legacy java.util.Date / SimpleDateFormat with java.time API.
// java.util.Date and SimpleDateFormat are not thread-safe and are legacy APIs.
// java.time.LocalDateTime and DateTimeFormatter are the modern replacements (Java 8+).
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // NOTE: Hardcoded absolute path — /var/legacy/reports does not exist in a Docker container.
    // Consider using volume mounts, cloud object storage (S3 / Azure Blob), or environment variable.
    private static final String REPORT_BASE_PATH = "/var/legacy/reports/";

    // NOTE: Windows-style absolute path will fail on any Linux-based container or cloud host.
    // Consider using environment variable or cloud storage for backup paths.
    private static final String BACKUP_PATH = "C:\\ResortBackups\\nightly\\";

    // NOTE: Fixed server port hardcoded in application logic.
    // Container orchestration (ECS / EKS) dynamically assigns ports.
    // Consider using ${server.port} from application.properties instead.
    private static final int SERVER_PORT = 8080;

    /**
     * Generates a monthly booking report as a CSV file.
     *
     * @param month the month for the report (e.g., "March")
     * @param year  the year for the report (e.g., "2024")
     * @return a map containing the report generation status and file path
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String fullPath = REPORT_BASE_PATH + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            File reportDir = new File(REPORT_BASE_PATH);
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }

            FileWriter writer = new FileWriter(fullPath);
            writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            writer.close();

            result.put("status", "generated");
            result.put("path", fullPath);
            result.put("serverPort", SERVER_PORT);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds the download URL for a given report name.
     *
     * @param reportName the name of the report file
     * @return the full download URL for the report
     */
    public String buildReportDownloadUrl(String reportName) {
        // NOTE: Plain HTTP URL hardcoded for report download.
        // Cloud security standards enforce HTTPS. Consider updating to HTTPS endpoint.
        return "http://reports.resorts-internal.com:8080/download/" + reportName;
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
        info.put("reportPath", REPORT_BASE_PATH);
        info.put("backupPath", BACKUP_PATH);
        info.put("serverPort", SERVER_PORT);
        info.put("generatedAt", timestamp);
        return info;
    }
}
