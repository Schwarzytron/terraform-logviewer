package ru.konkurst1.ekb.terraform_logviewer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;
import ru.konkurst1.ekb.terraform_logviewer.model.LogParseResult;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LogParserServiceTest {

    private LogParserService logParserService;

    @BeforeEach
    void setUp() {
        logParserService = new LogParserService();
    }

    @Test
    void parseLogs_WithValidLines_ShouldReturnEntries() {
        // Arrange
        List<String> lines = Arrays.asList(
            "2024-01-15 10:30:00 [INFO] Terraform will perform the following actions",
            "2024-01-15 10:30:01 [ERROR] Failed to create resource",
            "2024-01-15 10:30:02 [WARN] Deprecated feature used"
        );
        String logFileId = "test-file-1";

        // Act
        LogParseResult result = logParserService.parseLogs(lines, logFileId);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.entries().size());
        assertEquals(0, result.errors().size());
        
        LogEntry firstEntry = result.entries().get(0);
        assertEquals("plan", firstEntry.getSection());
        assertEquals(LogLevel.INFO, firstEntry.getLevel());
    }

    @Test
    void parseLogs_WithJsonData_ShouldExtractFields() {
        // Arrange
        List<String> lines = Arrays.asList(
            "2024-01-15 10:30:00 {\"tf_req_id\":\"test-req-123\",\"tf_resource_type\":\"t1_vpc_network\"}",
            "2024-01-15 10:30:01 {\"tf_req_id\":\"test-req-456\",\"tf_rpc\":\"ApplyResourceChange\"}"
        );
        String logFileId = "test-file-2";

        // Act
        LogParseResult result = logParserService.parseLogs(lines, logFileId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.entries().size());
        
        LogEntry firstEntry = result.entries().get(0);
        assertEquals("test-req-123", firstEntry.getTfReqId());
        assertEquals("t1_vpc_network", firstEntry.getTfResourceType());
        assertTrue(firstEntry.getHasJson());
        
        LogEntry secondEntry = result.entries().get(1);
        assertEquals("request", secondEntry.getRequestType());
    }

    @Test
    void parseLogs_WithInvalidLines_ShouldCreateErrorEntries() {
        // Arrange
        List<String> lines = Arrays.asList(
            "valid line with timestamp",
            "", // empty line
            "another valid line"
        );
        String logFileId = "test-file-3";

        // Act
        LogParseResult result = logParserService.parseLogs(lines, logFileId);

        // Assert
        assertNotNull(result);
        assertTrue(result.entries().size() >= 2);
        // Should handle empty lines without throwing exceptions
    }

    @Test
    void parseTimestamp_WithDifferentFormats_ShouldParseCorrectly() {
        // Act & Assert
        assertNotNull(LogParserService.parseTimestamp("2024-01-15 10:30:00"));
        assertNotNull(LogParserService.parseTimestamp("2024-01-15T10:30:00"));
        assertNotNull(LogParserService.parseTimestamp("10:30:00"));
    }

    @Test
    void detectLogLevel_WithVariousKeywords_ShouldDetectCorrectLevel() {
        List<String> lines = Arrays.asList(
            "error: something went wrong",
            "warning: this is deprecated",
            "info: creating resource",
            "debug: detailed information"
        );

        LogParseResult result = logParserService.parseLogs(lines, "test");
        
        assertEquals(LogLevel.ERROR, result.entries().get(0).getLevel());
        assertEquals(LogLevel.WARN, result.entries().get(1).getLevel());
    }
}