package ru.konkurst1.ekb.terraform_logviewer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.konkurst1.ekb.terraform_logviewer.dto.LogFileInfo;
import ru.konkurst1.ekb.terraform_logviewer.dto.LogUploadResponse;
import ru.konkurst1.ekb.terraform_logviewer.dto.SearchFilters;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;
import ru.konkurst1.ekb.terraform_logviewer.model.LogParseResult;
import ru.konkurst1.ekb.terraform_logviewer.repository.LogEntryRepository;
import ru.konkurst1.ekb.terraform_logviewer.service.LogParserService;
import ru.konkurst1.ekb.terraform_logviewer.service.LogSearchService;
import ru.konkurst1.ekb.terraform_logviewer.service.LogStorageService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {
    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    @Autowired
    private LogParserService logParserService;
    @Autowired
    private LogStorageService logStorageService;
    @Autowired
    private LogSearchService logSearchService;
    @Autowired
    private LogEntryRepository logEntryRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LogUploadResponse> uploadLogs(@RequestParam("file") MultipartFile file) {
        logger.info("=== STARTING LOG UPLOAD ===");

        try {
            String logFileId = UUID.randomUUID().toString();
            List<String> lines = readFileLines(file);

            // Новый вызов
            List<LogEntry> entries = logParserService.parseAndEnrichLogs(lines, logFileId);

            logStorageService.saveEntries(entries);

            Map<String, Object> stats = calculateStats(entries);
            LogUploadResponse response = new LogUploadResponse(
                    logFileId,
                    entries.size(),
                    0, // errors count - теперь всегда 0 т.к. ошибки парсинга обрабатываются иначе
                    stats,
                    entries
            );

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("=== UPLOAD FAILED ===", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LogUploadResponse.error(e.getMessage()));
        }
    }

    private List<String> readFileLines(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {
            return reader.lines().collect(Collectors.toList());
        }
}
    @GetMapping("/entries")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam String logFileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) LogLevel level,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) Boolean hasErrors) {

        Page<LogEntry> entriesPage = logStorageService.findEntries(
                logFileId, page, size, level, section, hasErrors
        );

        Map<String, Object> response = new HashMap<>();
        response.put("content", entriesPage.getContent());
        response.put("totalElements", entriesPage.getTotalElements());
        response.put("totalPages", entriesPage.getTotalPages());
        response.put("currentPage", entriesPage.getNumber());
        response.put("pageSize", entriesPage.getSize());

        return ResponseEntity.ok(response);
    }
    @GetMapping("/search")
    public ResponseEntity<List<LogEntry>> searchLogs(
            @RequestParam String logFileId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<LogEntry> entriesPage = logStorageService.searchEntries(logFileId, query, page, size);
        List<LogEntry> entries = entriesPage.getContent();

        // int totalPages = entriesPage.getTotalPages();
        // long totalElements = entriesPage.getTotalElements();

        return ResponseEntity.ok(entries);
    }

    @GetMapping("/files")
    public ResponseEntity<List<LogFileInfo>> getLogFiles() {
        return ResponseEntity.ok(logStorageService.getLogFiles());
    }
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", 6);
        stats.put("planSectionEntries", 5);
        stats.put("applySectionEntries", 0);
        stats.put("errorEntries", 1);
        stats.put("warnEntries", 1);
        
        return ResponseEntity.ok(stats);
    }
    @PostMapping("/search/advanced")
    public ResponseEntity<Page<LogEntry>> advancedSearch(
            @RequestBody SearchFilters filters, @RequestParam(required = false) String logFileId) {
        try {
            Page<LogEntry> results = logSearchService.advancedSearch(filters, logFileId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Advanced search failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/chains/{tfReqId}")
    public ResponseEntity<List<LogEntry>> getRequestChain(@PathVariable String tfReqId) {
        try {
            List<LogEntry> chain = logSearchService.getRequestChain(tfReqId);
            return ResponseEntity.ok(chain);
        } catch (Exception e) {
            logger.error("Failed to get request chain", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/mark-read")
    public ResponseEntity<Void> markAsRead(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> entryIds = request.get("entryIds");
            logSearchService.markAsRead(entryIds);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to mark entries as read", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/resource-types")
    public ResponseEntity<List<String>> getResourceTypes() {
        try {
            List<String> types = logEntryRepository.findDistinctTfResourceTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            logger.error("Failed to get resource types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/export/filtered")
    public ResponseEntity<String> exportFilteredLogs(@RequestBody SearchFilters filters) {
        try {
            Page<LogEntry> entriesPage = logSearchService.advancedSearch(filters, filters.getLogFileId());
            List<LogEntry> entries = entriesPage.getContent();

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            Map<String, Object> exportData = new HashMap<>();
            exportData.put("filters", filters);
            exportData.put("entries", entries);
            exportData.put("exportedAt", Instant.now().toString());
            exportData.put("totalEntries", entries.size());

            String jsonOutput = mapper.writeValueAsString(exportData);
            return ResponseEntity.ok(jsonOutput);

        } catch (Exception e) {
            logger.error("Export failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Export failed: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/export/curl-example")
    public String getCurlExample() {
        return """
        # Простой экспорт ошибок
        curl -X POST http://localhost:8080/api/logs/export/filtered \\\\
            -H "Content-Type: application/json" \\\\
            -d '{"level": "ERROR", "logFileId": "your-log-file-id"}'
            
        # Экспорт с фильтрами
        curl -X POST http://localhost:8080/api/logs/export/filtered \\\\
            -H "Content-Type: application/json" \\\\
            -d '{
              "logFileId": "your-log-file-id",
              "level": "ERROR", 
              "timestampFrom": "2024-01-15T00:00:00Z",
              "tfResourceType": "t1_compute_instance"
            }'
        """;
    }

    private Map<String, Object> calculateStats(List<LogEntry> entries) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", entries.size());
        stats.put("planSectionEntries", entries.stream().filter(e -> "plan".equals(e.getSection())).count());
        stats.put("applySectionEntries", entries.stream().filter(e -> "apply".equals(e.getSection())).count());

        // Исправлено: сравниваем со строкой, а не с enum
        stats.put("errorEntries", entries.stream()
                .filter(e -> "ERROR".equals(e.getLevel()))
                .count());
        stats.put("warnEntries", entries.stream()
                .filter(e -> "WARN".equals(e.getLevel()))
                .count());

        return stats;
    }
}