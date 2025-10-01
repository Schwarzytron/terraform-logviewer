package ru.konkurst1.ekb.terraform_logviewer.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.konkurst1.ekb.terraform_logviewer.dto.LogUploadResponse;
import ru.konkurst1.ekb.terraform_logviewer.dto.SearchFilters;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;
import ru.konkurst1.ekb.terraform_logviewer.model.LogParseResult;
import ru.konkurst1.ekb.terraform_logviewer.repository.LogEntryRepository;
import ru.konkurst1.ekb.terraform_logviewer.service.LogParserService;
import ru.konkurst1.ekb.terraform_logviewer.service.LogSearchService;
import ru.konkurst1.ekb.terraform_logviewer.service.LogStorageService;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogControllerTest {

    @Mock
    private LogParserService logParserService;

    @Mock
    private LogStorageService logStorageService;

    @Mock
    private LogSearchService logSearchService;

    @Mock
    private LogEntryRepository logEntryRepository;

    @InjectMocks
    private LogController logController;

    @Test
    void uploadLogs_WithValidFile_ShouldReturnSuccessResponse() throws Exception {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "test.log", 
            "test.log", 
            "text/plain", 
            "2024-01-15 10:30:00 [INFO] Test log line".getBytes()
        );

        List<LogEntry> parsedEntries = List.of(createTestLogEntry());
        LogParseResult parseResult = new LogParseResult(parsedEntries, List.of());

        when(logParserService.parseLogs(anyList(), anyString())).thenReturn(parseResult);
        doNothing().when(logStorageService).saveEntries(anyList());

        // Act
        ResponseEntity<LogUploadResponse> response = logController.uploadLogs(file);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getError());
        verify(logParserService, times(1)).parseLogs(anyList(), anyString());
        verify(logStorageService, times(1)).saveEntries(anyList());
    }

    @Test
    void uploadLogs_WithIOException_ShouldReturnErrorResponse() throws Exception {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.log");
        when(file.getInputStream()).thenThrow(new IOException("File read error"));

        // Act
        ResponseEntity<LogUploadResponse> response = logController.uploadLogs(file);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getError());
    }

    @Test
    void getLogs_WithParameters_ShouldReturnPagedEntries() {
        // Arrange
        String logFileId = "test-file";
        Page<LogEntry> expectedPage = new PageImpl<>(Arrays.asList(createTestLogEntry()));

        when(logStorageService.findEntries(eq(logFileId), anyInt(), anyInt(), any(), any(), any()))
            .thenReturn(expectedPage);

        // Act
        ResponseEntity<Page<LogEntry>> response = logController.getLogs(logFileId, 0, 50, null, null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    void advancedSearch_WithFilters_ShouldReturnResults() {
        // Arrange
        SearchFilters filters = new SearchFilters();
        filters.setFreeText("test");
        Page<LogEntry> expectedPage = new PageImpl<>(List.of(createTestLogEntry()));

        when(logSearchService.advancedSearch(filters, null)).thenReturn(expectedPage);

        // Act
        ResponseEntity<Page<LogEntry>> response = logController.advancedSearch(filters, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getRequestChain_WithValidRequestId_ShouldReturnChain() {
        // Arrange
        String requestId = "test-req";
        List<LogEntry> expectedChain = List.of(createTestLogEntry());

        when(logSearchService.getRequestChain(requestId)).thenReturn(expectedChain);

        // Act
        ResponseEntity<List<LogEntry>> response = logController.getRequestChain(requestId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void markAsRead_WithEntryIds_ShouldMarkEntries() {
        // Arrange
        List<String> entryIds = Arrays.asList("1L", "2L", "3L");

        // Act
        ResponseEntity<Void> response = logController.markAsRead(Map.of("entryIds", entryIds));

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(logSearchService, times(1)).markAsRead(entryIds);
    }

    @Test
    void getResourceTypes_ShouldReturnDistinctTypes() {
        // Arrange
        List<String> expectedTypes = Arrays.asList("t1_vpc_network", "t1_instance");

        when(logEntryRepository.findDistinctTfResourceTypes()).thenReturn(expectedTypes);

        // Act
        ResponseEntity<List<String>> response = logController.getResourceTypes();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    private LogEntry createTestLogEntry() {
        LogEntry entry = new LogEntry();
        entry.setId("1L");
        entry.setRawMessage("Test message");
        entry.setTimestamp(Instant.now());
        entry.setLevel(LogLevel.INFO);
        entry.setSection("plan");
        entry.setMessage("Test message");
        entry.setLogFileId("test-file");
        return entry;
    }
}