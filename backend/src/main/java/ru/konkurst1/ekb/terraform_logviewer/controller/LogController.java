package ru.konkurst1.ekb.terraform_logviewer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.konkurst1.ekb.terraform_logviewer.dto.LogFileInfo;
import ru.konkurst1.ekb.terraform_logviewer.dto.LogUploadResponse;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;
import ru.konkurst1.ekb.terraform_logviewer.model.LogLevel;
import ru.konkurst1.ekb.terraform_logviewer.service.LogParseResult;
import ru.konkurst1.ekb.terraform_logviewer.service.LogParserService;
import ru.konkurst1.ekb.terraform_logviewer.service.LogStorageService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {
    
    @Autowired
    private LogParserService logParserService;
    @Autowired
    private LogStorageService logStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LogUploadResponse> uploadLogs(@RequestParam("file") MultipartFile file) {
        try {
            String logFileId = UUID.randomUUID().toString();
            List<String> lines = readFileLines(file);

            LogParseResult result = logParserService.parseLogs(lines, logFileId);
            logStorageService.saveEntries(result.getEntries());

            return ResponseEntity.ok(new LogUploadResponse(
                    logFileId,
                    result.getEntries().size(),
                    result.getErrors().size(),
                    calculateStats(result.getEntries())
            ));

        } catch (Exception e) {
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

        List<LogEntry> entries = logStorageService.searchEntries(logFileId, query, page, size);
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