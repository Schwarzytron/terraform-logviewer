package ru.konkurst1.ekb.terraform_logviewer.dto;

import lombok.Getter;
import ru.konkurst1.ekb.terraform_logviewer.model.LogEntry;

import java.util.List;
import java.util.Map;

@Getter
public class LogUploadResponse {
    // getters and setters
    private String logFileId;
    private Integer entriesProcessed;
    private Integer errorsCount;
    private Map<String, Object> stats;
    List<LogEntry> entries;
    private String error;

    // Success constructor
    public LogUploadResponse(String logFileId, Integer entriesProcessed, 
                           Integer errorsCount, Map<String, Object> stats,
                             List<LogEntry> entries) {
        this.logFileId = logFileId;
        this.entriesProcessed = entriesProcessed;
        this.errorsCount = errorsCount;
        this.stats = stats;
        this.entries = entries;
    }
    
    // Error constructor
    public LogUploadResponse(String error) {
        this.error = error;
    }
    
    // Static factory method for errors
    public static LogUploadResponse error(String message) {
        return new LogUploadResponse(message);
    }

}