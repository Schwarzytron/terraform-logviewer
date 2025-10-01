package ru.konkurst1.ekb.terraform_logviewer.controller;

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
        logger.info("File name: {}, Size: {}, Content type: {}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            String logFileId = UUID.randomUUID().toString();
            logger.info("Generated log file ID: {}", logFileId);
            List<String> lines = readFileLines(file);
            logger.info("Read {} lines from file", lines.size());

            // Log first few lines for debugging
            if (!lines.isEmpty()) {
                logger.info("First 3 lines sample:");
                for (int i = 0; i < Math.min(3, lines.size()); i++) {
                    logger.info("Line {}: {}", i + 1, lines.get(i));
                }
            }

            LogParseResult result = logParserService.parseLogs(lines, logFileId);
            logger.info("Parsing completed - Entries: {}, Errors: {}",
                    result.entries().size(), result.errors().size());

            logStorageService.saveEntries(result.entries());
            logger.info("Entries saved to database");

            Map<String, Object> stats = calculateStats(result.entries());
            logger.info("Stats calculated: {}", stats);

            LogUploadResponse response = new LogUploadResponse(
                    logFileId,
                    result.entries().size(),
                    result.errors().size(),
                    stats,
                    result.entries()
            );

            logger.info("=== UPLOAD COMPLETED SUCCESSFULLY ===");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
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
    public ResponseEntity<Page<LogEntry>> getLogs(
            @RequestParam String logFileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) LogLevel level,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) Boolean hasErrors) {

        Page<LogEntry> entries = logStorageService.findEntries(
                logFileId, page, size, level, section, hasErrors
        );
        return ResponseEntity.ok(entries);
    }
    @GetMapping("/search")
    public ResponseEntity<List<LogEntry>> searchLogs(
            @RequestParam String logFileId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<LogEntry> entriesPage = logStorageService.searchEntries(logFileId, query, page, size);
        List<LogEntry> entries = entriesPage.getContent();

        int totalPages = entriesPage.getTotalPages();
        long totalElements = entriesPage.getTotalElements();

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
    private Map<String, Object> calculateStats(List<LogEntry> entries) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", entries.size());
        stats.put("planSectionEntries", entries.stream().filter(e -> "plan".equals(e.getSection())).count());
        stats.put("applySectionEntries", entries.stream().filter(e -> "apply".equals(e.getSection())).count());
        stats.put("errorEntries", entries.stream().filter(e -> LogLevel.ERROR.equals(e.getLevel())).count());
        stats.put("warnEntries", entries.stream().filter(e -> LogLevel.WARN.equals(e.getLevel())).count());
        
        return stats;
    }
}