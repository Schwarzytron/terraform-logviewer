package ru.konkurst1.ekb.terraform_logviewer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import ru.konkurst1.ekb.terraform_logviewer.dto.LogFileInfo;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;
import ru.konkurst1.ekb.terraform_logviewer.repository.LogEntryRepository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogStorageServiceTest {

    @Mock
    private LogEntryRepository logEntryRepository;

    @InjectMocks
    private LogStorageService logStorageService;

    @Test
    void saveEntries_WithValidEntries_ShouldSaveToRepository() {
        // Arrange
        List<LogEntry> entries = Arrays.asList(
                createTestLogEntry("1L"),
                createTestLogEntry("2L")
        );

        when(logEntryRepository.saveAll(entries)).thenReturn(entries);

        // Act
        logStorageService.saveEntries(entries);

        // Assert
        verify(logEntryRepository, times(1)).saveAll(entries);
    }

    @Test
    void searchEntries_WithQuery_ShouldReturnMatchingEntries() {
        // Arrange
        String logFileId = "test-file";
        String query = "error";
        Pageable pageable = PageRequest.of(0, 10);
        List<LogEntry> expectedEntries = List.of(createTestLogEntry("1L"));
        Page<LogEntry> expectedPage = new PageImpl<>(expectedEntries, pageable, 1);

        when(logEntryRepository.findByLogFileIdAndMessageContainingIgnoreCase(eq(logFileId), eq(query), any(Pageable.class)))
                .thenReturn(expectedPage);

        // Act
        Page<LogEntry> result = logStorageService.searchEntries(logFileId, query, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getLogFiles_ShouldReturnFileInfo() {
        // Arrange
        List<LogFileInfo> expectedFiles = Arrays.asList(
                new LogFileInfo("file1", Instant.now(), 100L, 2L),
                new LogFileInfo("file2", Instant.now(), 50L, 1L)
        );

        when(logEntryRepository.findLogFileInfo()).thenReturn(expectedFiles);

        // Act
        List<LogFileInfo> result = logStorageService.getLogFiles();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(logEntryRepository, times(1)).findLogFileInfo();
    }

    private LogEntry createTestLogEntry(String id) {
        LogEntry entry = new LogEntry();
        entry.setId(id);
        entry.setRawMessage("Test message " + id);
        entry.setTimestamp(Instant.now());
        entry.setLevel(LogLevel.INFO);
        entry.setSection("plan");
        entry.setMessage("Test message");
        entry.setLogFileId("test-file");
        return entry;
    }
}